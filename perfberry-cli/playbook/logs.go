package playbook

import (
	"io/ioutil"
	"log"
	"strconv"

	"github.com/tbushueva/perfberry/perfberry-cli/api"
	"github.com/tbushueva/perfberry/perfberry-cli/handlers"
	"github.com/tbushueva/perfberry/perfberry-cli/models"
	"github.com/tbushueva/perfberry/perfberry-cli/ui"
	"github.com/go-yaml/yaml"
)

type LogsUploadOptions struct {
	ProjectID      int               `yaml:"project-id"`
	ReportFile     string            `yaml:"report-file"`
	BuildFile      string            `yaml:"build-file"`
	AssertionsFile string            `yaml:"assertions-file"`
	Dir            string            `yaml:"dir"`
	Type           string            `yaml:"type"`
	Extended       bool              `yaml:"extended"`
	ReportId       int               `yaml:"report-id"`
	ReportIdFile   string            `yaml:"report-id-file"`
	OutputFile     string            `yaml:"output-file"`
	FollowStatus   bool              `yaml:"follow-status"`
	Report         models.Report     `yaml:"report,omitempty"`
	Build          models.Build      `yaml:"build,omitempty"`
	Assertions     models.Assertions `yaml:"assertions,omitempty"`
}

type LogsUploadJob struct {
	Options LogsUploadOptions
}

func (j *LogsUploadJob) Run(ac *api.Client, uc *ui.Client) error {
	//TODO not save if report id provided
	reportFile := j.Options.ReportFile
	if reportFile == "" {
		path := j.Options.Report.Path()
		log.Println("Saving report options to", path, "...")
		err := j.Options.Report.ToFile(path)
		if err != nil {
			return err
		}
		reportFile = path
	}

	buildFile := j.Options.BuildFile
	if buildFile == "" {
		path := j.Options.Build.Path()
		log.Println("Saving build options to", path, "...")
		err := j.Options.Build.ToFile(path)
		if err != nil {
			return err
		}
		buildFile = path
	}

	assertionsFile := j.Options.AssertionsFile
	if assertionsFile == "" {
		path := j.Options.Assertions.Path()
		log.Println("Saving assertions options to", path, "...")
		err := j.Options.Assertions.ToFile(path)
		if err != nil {
			return err
		}
		assertionsFile = path
	}

	var reportId int
	if j.Options.ReportIdFile != "" {
		bts, err := ioutil.ReadFile(j.Options.ReportIdFile)
		if err != nil {
			return err
		}
		i, err := strconv.Atoi(string(bts))
		if err != nil {
			return err
		}
		reportId = i
	} else {
		reportId = j.Options.ReportId
	}

	return handlers.UploadLog(
		j.Options.ProjectID,
		j.Options.Type,
		j.Options.Extended,
		reportId,
		j.Options.Dir,
		reportFile,
		buildFile,
		assertionsFile,
		j.Options.OutputFile,
		j.Options.FollowStatus,
		ac,
		uc,
	)
}

func NewLogsUploadJob(data interface{}) (j *LogsUploadJob, err error) {
	options := &LogsUploadOptions{}

	bts, err := yaml.Marshal(data)
	if err != nil {
		return
	}

	err = yaml.Unmarshal(bts, options)
	if err != nil {
		return
	}

	return &LogsUploadJob{Options: *options}, nil
}
