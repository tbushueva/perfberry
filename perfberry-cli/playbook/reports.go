package playbook

import (
	"log"

	"github.com/tbushueva/perfberry/perfberry-cli/api"
	"github.com/tbushueva/perfberry/perfberry-cli/handlers"
	"github.com/tbushueva/perfberry/perfberry-cli/models"
	"github.com/tbushueva/perfberry/perfberry-cli/ui"
	"github.com/go-yaml/yaml"
)

type ReportsCreateOptions struct {
	ProjectID    int           `yaml:"project-id"`
	ReportFile   string        `yaml:"report-file"`
	OutputIdFile string        `yaml:"output-id-file"`
	OutputFile   string        `yaml:"output-file"`
	Report       models.Report `yaml:"report,omitempty"`
}

type ReportsCreateJob struct {
	Options ReportsCreateOptions
}

func (j *ReportsCreateJob) Run(ac *api.Client, uc *ui.Client) error {
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

	return handlers.CreateReport(
		j.Options.ProjectID,
		reportFile,
		j.Options.OutputIdFile,
		j.Options.OutputFile,
		ac,
		uc,
	)
}

func NewReportsCreateJob(data interface{}) (*ReportsCreateJob, error) {
	options := &ReportsCreateOptions{}

	bts, err := yaml.Marshal(data)
	if err != nil {
		return nil, err
	}

	err = yaml.Unmarshal(bts, options)
	if err != nil {
		return nil, err
	}

	return &ReportsCreateJob{Options: *options}, nil
}
