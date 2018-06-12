package com.dragon.gerrit.jenson.controller;

import com.dragon.gerrit.jenson.data.ParseSonarRule;
import com.dragon.gerrit.jenson.data.ParseStaticCheckReport;
import com.dragon.gerrit.jenson.data.converter.CustomIssueFormatter;
import com.dragon.gerrit.jenson.data.converter.CustomReportFormatter;
import com.dragon.gerrit.jenson.data.entity.ScrIssue;
import com.dragon.gerrit.jenson.data.entity.ScrReport;
import com.dragon.gerrit.jenson.data.entity.Severity;
import com.dragon.gerrit.jenson.data.entity.SonarRule;
import com.dragon.gerrit.jenson.data.entity.SubJobConfig;
import com.dragon.gerrit.jenson.data.predicates.ByMinSeverityPredicate;
import com.dragon.gerrit.jenson.util.Logger;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.*;
import com.google.gerrit.extensions.api.GerritApi;
import com.google.gerrit.extensions.api.changes.ReviewInput;
import com.google.gerrit.extensions.api.changes.RevisionApi;
import com.google.gerrit.extensions.common.DiffInfo;
import com.google.gerrit.extensions.common.FileInfo;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.sonyericsson.hudson.plugins.gerrit.trigger.GerritManagement;
import com.sonyericsson.hudson.plugins.gerrit.trigger.config.IGerritHudsonTriggerConfig;
import com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.GerritTrigger;
import com.urswolfer.gerrit.client.rest.GerritAuthData;
import com.urswolfer.gerrit.client.rest.GerritRestApiFactory;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import static com.dragon.gerrit.jenson.util.Localization.getLocalized;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;

/**
 * @project	: This Gerrit-Jinson plugin is used to post the review of Issues of static code check to
 * 			  the Gerrit server, but how to generate the issues report is not the task of current
 * 			  plugin. If you want to use this plugin, you should generate the issue report by the
 * 		   	  third tool. The format of issue report must contain 5 fields, they are file, line,
 * 			  id, severity and msg. The file is the source file. The line is the number of issue, 
 * 			  The id is the error key. The severity is the error level. The msg is the error message
 * 			  The owner of project can customize the error level, You can set in the SonarQube Settings.
 * 			  Default condition uses the error level of the third tool.
 */
public class ScrToGerritPublisher extends Publisher {
	
	private static final String GERRIT_NAME = "GERRIT_NAME";
	private static final String GERRIT_CHANGE_NUMBER = "GERRIT_CHANGE_NUMBER";
	private static final String GERRIT_PATCHSET_NUMBER = "GERRIT_PATCHSET_NUMBER";

    private static final String DEFAULT_CATEGORY = "Code-Review";
    private static final int DEFAULT_SCORE = 0;
    private static final ReviewInput.NotifyHandling DEFAULT_NOTIFICATION_NO_ISSUES = ReviewInput.NotifyHandling.NONE;
    private static final ReviewInput.NotifyHandling DEFAULT_NOTIFICATION_ISSUES = ReviewInput.NotifyHandling.OWNER;
	
    private final String sonarURL;
    private final String sonarRulePath;
    private final String severity;
    private final boolean changedLinesOnly;
    private final String noIssuesToPostText;
    private final String someIssuesToPostText;
    private final String issueComment;
    private final boolean postScore;
    private final String category;
    private final String noIssuesScore;
    private final String issuesScore;
    private final String noIssuesNotification;
    private final String issuesNotification;
    private List<SubJobConfig> subJobConfigs;
    
    private transient Logger logger;
    private boolean hasSonarRule = true;

	@DataBoundConstructor
    public ScrToGerritPublisher(String sonarURL, String sonarRulePath, String severity, 
    							boolean changedLinesOnly, String noIssuesToPostText, 
    							String someIssuesToPostText, String issueComment, 
    							boolean postScore, String category, 
    							String noIssuesScore, String issuesScore, 
    							String noIssuesNotification, String issuesNotification,
    							List<SubJobConfig> subJobConfigs) {
			this.sonarURL = sonarURL;
			this.sonarRulePath = sonarRulePath;
			this.severity = MoreObjects.firstNonNull(severity, Severity.MAJOR.name());;
			this.changedLinesOnly = changedLinesOnly;
			this.noIssuesToPostText = noIssuesToPostText;
			this.someIssuesToPostText = someIssuesToPostText;
			this.issueComment = issueComment;
			this.postScore = postScore;
			this.category = MoreObjects.firstNonNull(category, DEFAULT_CATEGORY);;
			this.noIssuesScore = noIssuesScore;
			this.issuesScore = issuesScore;
			this.noIssuesNotification = noIssuesNotification;
			this.issuesNotification = issuesNotification;
			this.subJobConfigs = subJobConfigs;
    }
	
    public String getSonarURL() {
        return sonarURL;
    }

    public String getSonarRulePath() {
        return sonarRulePath;
    }
    
    public String getSeverity() {
        return severity;
    }

    public boolean isChangedLinesOnly() {
        return changedLinesOnly;
    }

    public String getNoIssuesToPostText() {
        return noIssuesToPostText;
    }

    public String getSomeIssuesToPostText() {
        return someIssuesToPostText;
    }

    public String getIssueComment() {
        return issueComment;
    }

    public boolean isPostScore() {
        return postScore;
    }

    public String getCategory() {
        return category;
    }

    public String getNoIssuesScore() {
        return noIssuesScore;
    }
    
    public String getIssuesScore() {
        return issuesScore;
    }

    public String getNoIssuesNotification() {
        return noIssuesNotification;
    }

    public String getIssuesNotification() {
        return issuesNotification;
    }
    
    public List<SubJobConfig> getSubJobConfigs() {
    	return subJobConfigs;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
    	
    	logger = new Logger(listener);
    	
    	SonarRule sonarRules = readSonarRule(build);
    	if (sonarRules == null) {
    		hasSonarRule = false;
    	}

    	ScrReport scrReport = readScrReports(build);
    	if (scrReport == null) {
    		return false;
    	}
    	
    	Multimap<String, ScrIssue> fileMapIssue = null;
    	if (hasSonarRule) {
    		if (formatIssuesBySonarRules(scrReport, sonarRules) == false) {
    			return false;
    		}
    		fileMapIssue = generateFileMapIssueBySeverity(scrReport);
    	} else {
    		fileMapIssue = generateFileMapIssueByNothing(scrReport);
    	}
        
/*    	Iterator iter = fileMapIssue.entries().iterator();
        while (iter.hasNext()) {
        	Map.Entry<String, ScrIssue> entry = (Map.Entry<String, ScrIssue>)iter.next();
        	logger.d(entry.getKey() + " " + entry.getValue().toString());
        }
        logger.d("The total size = " + fileMapIssue.values().size()); */
        
        try {
        	RevisionApi revision = prepareForGerritResetApi(build, listener, fileMapIssue);
        	if (revision == null ) {
        		return false;
        	} else {
        		return reviewResultToGerrit(build, listener, revision, fileMapIssue);
        	}
		} catch (RestApiException e) {
			logger.f(String.format(getLocalized("jenkins.plugin.review.fail.sent"), e.getMessage()));
			return false;
		}
    }
    
    /**
     * @param  build
     * @return Returning the customer rule information
     * @throws IOException
     * @throws InterruptedException
     */
	private SonarRule readSonarRule(AbstractBuild build) throws IOException, InterruptedException {
		FilePath rulePath = build.getWorkspace().child(sonarRulePath);
		if (!rulePath.exists()) {
			logger.e(String.format(getLocalized("jenkins.plugin.error.sonar.rule.not.exists"), rulePath));
			return null;
		}
		logger.i(String.format(getLocalized("jenkins.plugin.getting.rule"), rulePath));
		ParseSonarRule parseSonarRule = new ParseSonarRule();
		SonarRule sonarRule = parseSonarRule.getSonarRules(rulePath.read(), logger);
		if (sonarRule == null) {
			return null;
		}
		logger.i(String.format(getLocalized("jenkins.plugin.rule.loaded"), sonarRule.getRules().size()));
		return sonarRule;
	}
	
	
	private ScrReport readScrReport(AbstractBuild build, SubJobConfig subJobConfig) throws IOException, InterruptedException {
		FilePath reportPath = build.getWorkspace().child(subJobConfig.getSonarReportPath());
		if (!reportPath.exists()) {
			logger.e(String.format(getLocalized("jenkins.plugin.error.sonar.report.not.exists"), reportPath));
			return null;
		}
		logger.i(String.format(getLocalized("jenkins.plugin.getting.report"), reportPath));
		
		ParseStaticCheckReport parseStaticCheckReport = new ParseStaticCheckReport();
		return parseStaticCheckReport.getScrReport(reportPath.read(), logger);
	}
	
	/**
	 * @param  build
	 * @param  sonarRules
	 * @return Returning the issues of static check report
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private  ScrReport readScrReports(AbstractBuild build) throws IOException, InterruptedException {
		if (subJobConfigs == null || subJobConfigs.size() == 0) {
			logger.e(getLocalized("jenkins.plugin.error.sonar.report.not.set"));
			return null;
		}
		
		ScrReport scrReport = new ScrReport();
        for (SubJobConfig subJobConfig : subJobConfigs) {
    		ScrReport report = readScrReport(build, subJobConfig);
    		if (report == null) {
    			return null;
    		}
    		scrReport.setVersion(report.getVersion());
			scrReport.getIssues().addAll(report.getIssues());
        }
        logger.i(String.format(getLocalized("jenkins.plugin.report.loaded"), scrReport.getIssues().size()));
        return scrReport;
	}
	
	/**
	 * @param  scrReport
	 * @param  sonarRules
	 * @return Format the issues of report by the sonar rules
	 */
	private boolean formatIssuesBySonarRules(ScrReport scrReport, SonarRule sonarRules) {
		for (ScrIssue issue : scrReport.getIssues()) {
			String severity = sonarRules.getRules().get(issue.getId());
			if (severity == null) {
				logger.i(String.format(getLocalized("jenkins.plugin.rule.mismapped"), issue.getId()));
				return false;
			}
			issue.setSeverity(severity);
		}
		return true;
	}
	
	/**
	 * @param  scrReport
	 * @return Returning the issues was filtered by error level
	 */
	private Multimap<String, ScrIssue> generateFileMapIssueBySeverity(ScrReport scrReport) {
		Multimap<String, ScrIssue> fileMapIssue = LinkedListMultimap.create();
		Severity sev = Severity.valueOf(severity);
		
		Iterable<ScrIssue> filtered = Iterables.filter(scrReport.getIssues(), Predicates.and(ByMinSeverityPredicate.apply(sev)));
		for (ScrIssue issue : filtered) {
			fileMapIssue.put(issue.getFile(), issue);
		}
		return fileMapIssue;
	}
	
	/**
	 * @param  scrReport
	 * @return Returning the issues was filtered by nothing
	 */
	private Multimap<String, ScrIssue> generateFileMapIssueByNothing(ScrReport scrReport) {
		Multimap<String, ScrIssue> fileMapIssue = LinkedListMultimap.create();
		
		for (ScrIssue issue : scrReport.getIssues()) {
			fileMapIssue.put(issue.getFile(), issue);
		}
		return fileMapIssue;
	}
	
	/**
	 * @param  build
	 * @param  listener
	 * @param  name
	 * @return Get the enviroment variables from the Gerrit Server.
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private String getEnviroment(AbstractBuild build, BuildListener listener, String name) throws IOException, InterruptedException {
		EnvVars envVars = build.getEnvironment(listener);
		return envVars.get(name);
	}
	
	/**
	 * @param  build
	 * @param  listener
	 * @param  fileMapIssue
	 * @return Preparing Gerrit Reset API for reviewing
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws RestApiException
	 */
	public RevisionApi prepareForGerritResetApi(AbstractBuild build, BuildListener listener, Multimap<String, ScrIssue> fileMapIssue) throws IOException, InterruptedException, RestApiException {
		String gerritName = getEnviroment(build, listener, GERRIT_NAME);
		GerritTrigger trigger = GerritTrigger.getTrigger(build.getProject());
		String gerritServerName = gerritName != null ? gerritName : trigger != null ? trigger.getServerName() : null; 
		if (gerritServerName == null) {
			logger.e(getLocalized("jenkins.plugin.error.gerrit.server.empty"));
			return null;
		}
		
		IGerritHudsonTriggerConfig gerritConfig = GerritManagement.getConfig(gerritServerName);
		if (gerritConfig == null) {
			logger.e(getLocalized("jenkins.plugin.error.gerrit.config.empty"));
			return null;
		}
		
        if (!gerritConfig.isUseRestApi()) {
        	logger.e(getLocalized("jenkins.plugin.error.gerrit.restapi.off"));
            return null;
        }
        if (gerritConfig.getGerritHttpUserName() == null) {
        	logger.e(getLocalized("jenkins.plugin.error.gerrit.user.empty"));
            return null;
        }
        
        GerritAuthData.Basic authData = new GerritAuthData.Basic(gerritConfig.getGerritFrontEndUrl(),
                gerritConfig.getGerritHttpUserName(), gerritConfig.getGerritHttpPassword());
        GerritRestApiFactory gerritRestApiFactory = new GerritRestApiFactory();
        GerritApi gerritApi = gerritRestApiFactory.create(authData);
        
        int changeNumber = Integer.parseInt(getEnviroment(build, listener, GERRIT_CHANGE_NUMBER));
        int patchSetNumber = Integer.parseInt(getEnviroment(build, listener, GERRIT_PATCHSET_NUMBER));
        RevisionApi revision = gerritApi.changes().id(changeNumber).revision(patchSetNumber);
        logger.i(String.format(getLocalized("jenkins.plugin.connected.to.gerrit"), gerritServerName, changeNumber, patchSetNumber));
        
        return revision;
	}
	
	/**
	 * @param  listener
	 * @param  revision
	 * @param  fileMapIssue
	 * @return Reviewing the result to the gerrit server
	 * @throws RestApiException
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	private boolean reviewResultToGerrit(AbstractBuild build, BuildListener listener, RevisionApi revision, Multimap<String, ScrIssue> fileMapIssue) throws RestApiException, IOException {
		fileMapIssue = filerKeysByChangedFile(fileMapIssue, revision);
	
        if (isChangedLinesOnly()) {
        	filterIssuesByChangedLines(fileMapIssue, revision);
        }
        
        ReviewInput reviewInput = getReviewResult(build, listener, fileMapIssue);
        revision.review(reviewInput);
        logger.i(getLocalized("jenkins.plugin.review.sent"));
        
        return true;
	}
	
	/**
	 * @param  fileMapIssue
	 * @param  revision
	 * @return Filtering the issues by the changed file
	 * @throws RestApiException
	 */
	private Multimap<String, ScrIssue> filerKeysByChangedFile(Multimap<String, ScrIssue> fileMapIssue, RevisionApi revision) throws RestApiException {
		final Map<String, FileInfo> files = revision.files();
		fileMapIssue = Multimaps.filterKeys(fileMapIssue, new Predicate<String>() {
            @Override
            public boolean apply(String input) {
                return input != null && files.keySet().contains(input);
            }
        });
		return fileMapIssue;
	}
	
	/**
	 * @param  finalIssues
	 * @param  revision
	 * @throws RestApiException
	 */
	private void filterIssuesByChangedLines(Multimap<String, ScrIssue> finalIssues, RevisionApi revision) throws RestApiException {
        for (String filename : new HashSet<String>(finalIssues.keySet())) {
            List<DiffInfo.ContentEntry> content = revision.file(filename).diff().content;
            int processed = 0;
            Set<Integer> rangeSet = new HashSet<Integer>();
            
            for (DiffInfo.ContentEntry contentEntry : content) {
                if (contentEntry.ab != null) {
                    processed += contentEntry.ab.size();
                } else if (contentEntry.b != null) {
                    int start = processed + 1;
                    int end = processed + contentEntry.b.size();
                    for (int i = start; i <= end; i++) {
                        rangeSet.add(i);
                    }
                    processed += contentEntry.b.size();
                }
            }

            Collection<ScrIssue> issues = new ArrayList<ScrIssue>(finalIssues.get(filename));
            for (ScrIssue i : issues) {
                if (!rangeSet.contains(i.getLine())) {
                    finalIssues.get(filename).remove(i);
                }
            }
        }
	}
	
	/**
	 * @param  build
	 * @param  listener
	 * @param  finalIssues
	 * @return Returning the review message.
	 * @throws IOException
	 */
    @VisibleForTesting
    ReviewInput getReviewResult(AbstractBuild build, BuildListener listener, Multimap<String, ScrIssue> finalIssues) throws IOException {
    	String reviewMessage = null;
    	if (hasSonarRule == true) { 
        	reviewMessage = getReviewMessage(finalIssues);
    	} else {
    		reviewMessage = String.format(getLocalized("jenkins.plugin.review.message.nosonarQube.violation"), finalIssues.values().size());
    	}
    	ReviewInput reviewInput = new ReviewInput().message(reviewMessage);

        int finalIssuesCount = finalIssues.size();
        reviewInput.notify = getNotificationSettings(finalIssuesCount);

        if (postScore) {
            reviewInput.label(category, getReviewMark(finalIssuesCount));
        }
        
        reviewInput.comments = new HashMap<String, List<ReviewInput.CommentInput>>();
        for (String file : finalIssues.keySet()) {
            reviewInput.comments.put(file, Lists.newArrayList(
                    Collections2.transform(finalIssues.get(file),
                        new Function<ScrIssue, ReviewInput.CommentInput>() {
                            @Override
                            public ReviewInput.CommentInput apply(ScrIssue input) {
                                if (input == null) {
                                    return null;
                                }
                                ReviewInput.CommentInput commentInput = new ReviewInput.CommentInput();                          
                                commentInput.id = input.getId();
                                commentInput.line = input.getLine();
                                commentInput.message = new CustomIssueFormatter(input, issueComment, getSonarURL()).getMessage();
                                return commentInput;
                            }
                        }
                    )
                )
            );
        }

        return reviewInput;
    }
	
    /**
     * @param  finalIssues
     * @return Return the output format of report.
     */
    private String getReviewMessage(Multimap<String, ScrIssue> finalIssues) {
        return new CustomReportFormatter(finalIssues.values(), someIssuesToPostText, noIssuesToPostText).getMessage();
    }
	
    /**
     * @param  finalIssuesCount
     * @return Notification 
     */
    private ReviewInput.NotifyHandling getNotificationSettings(int finalIssuesCount) {
        if (finalIssuesCount > 0) {
            ReviewInput.NotifyHandling value = (issuesNotification == null ? null : ReviewInput.NotifyHandling.valueOf(issuesNotification));
            return MoreObjects.firstNonNull(value, DEFAULT_NOTIFICATION_ISSUES);
        } else {
            ReviewInput.NotifyHandling value = (noIssuesNotification == null ? null : ReviewInput.NotifyHandling.valueOf(noIssuesNotification));
            return MoreObjects.firstNonNull(value, DEFAULT_NOTIFICATION_NO_ISSUES);
        }
    }
	
    /**
     * @param  finalIssuesCount
     * @return Posting score to the Gerrit Server
     */
    private int getReviewMark(int finalIssuesCount) {
        String mark = finalIssuesCount > 0 ? issuesScore : noIssuesScore;
        return parseNumber(mark, DEFAULT_SCORE);
    }
	
    private int parseNumber(String number, int deflt) {
        try {
        	return Integer.parseInt(number);
        } catch (NumberFormatException e) {
            return deflt;
        }
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }
    
	@Override
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}
    
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        public DescriptorImpl() {
            load();
        }
        
        public FormValidation doCheckSonarURL(@QueryParameter String value) throws IOException, ServletException {
            if (value.length() == 0) {
                return FormValidation.warning(getLocalized("jenkins.plugin.validation.sonar.url.empty"));
            }
            try {
                new URL(value);
            } catch (MalformedURLException e) {
                return FormValidation.warning(getLocalized("jenkins.plugin.validation.sonar.url.invalid"));
            }
            return FormValidation.ok();
        }
        
        public FormValidation doCheckSonarRulePath(@QueryParameter String value) {
            return FormValidation.ok();
        }
        
        public FormValidation doCheckSeverity(@QueryParameter String value) {
            if (value == null || Severity.valueOf(value) == null) {
                return FormValidation.error(getLocalized("jenkins.plugin.validation.review.severity.unknown"));
            }
            return FormValidation.ok();
        }
        
        public FormValidation doCheckNoIssuesToPostText(@QueryParameter String value) {
            if (value.length() == 0) {
                return FormValidation.error(getLocalized("jenkins.plugin.validation.review.title.empty"));
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckSomeIssuesToPostText(@QueryParameter String value) {
            if (value.length() == 0) {
                return FormValidation.error(getLocalized("jenkins.plugin.validation.review.title.empty"));
            }
            return FormValidation.ok();
        }
        
        public FormValidation doCheckIssueComment(@QueryParameter String value) {
            if (value.length() == 0) {
                return FormValidation.error(getLocalized("jenkins.plugin.validation.review.body.empty"));
            }
            return FormValidation.ok();
        }
        
        public FormValidation doCheckIssuesScore(@QueryParameter String value) {
            return checkScore(value);
        }
        
        public FormValidation doCheckNoIssuesScore(@QueryParameter String value) {
            return checkScore(value);
        }
        
        public FormValidation doCheckNoIssuesNotification(@QueryParameter String value) {
            return checkNotificationType(value);
        }
        
        public FormValidation doCheckIssuesNotification(@QueryParameter String value) {
            return checkNotificationType(value);
        }
            
        private FormValidation checkScore(@QueryParameter String value) {
            try {
                Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return FormValidation.error(getLocalized("jenkins.plugin.validation.review.score.not.numeric"));
            }
            return FormValidation.ok();
        }
        
        private FormValidation checkNotificationType(@QueryParameter String value) {
            if (value == null || ReviewInput.NotifyHandling.valueOf(value) == null) {
                return FormValidation.error(getLocalized("jenkins.plugin.validation.review.notification.recipient.unknown"));
            }
            return FormValidation.ok();
        }
        
		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}

        @Override
        public String getDisplayName() {
            return getLocalized("jenkins.plugin.build.post.build.step.name");
        }
    }
    
}

