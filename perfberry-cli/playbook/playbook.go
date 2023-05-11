package playbook

import (
	"errors"
	"log"

	"github.com/tbushueva/perfberry/perfberry-cli/api"
	"github.com/tbushueva/perfberry/perfberry-cli/ui"
)

type Playbook struct {
	Jobs      []Jober
	ApiClient *api.Client
	UiClient  *ui.Client
}

type optFuncs map[string]map[string]func(options interface{}) (Jober, error)

var funcs = optFuncs{
	"reports": {
		"create": func(options interface{}) (Jober, error) {
			return NewReportsCreateJob(options)
		},
	},
	"logs": {
		"upload": func(options interface{}) (Jober, error) {
			return NewLogsUploadJob(options)
		},
	},
}

func NewPlaybook(config *PlaybookConfig, ac *api.Client, uc *ui.Client) (*Playbook, error) {
	playbook := &Playbook{
		ApiClient: ac,
		UiClient:  uc,
	}

	for _, job := range config.Jobs {
		for commandName, subcommand := range job {
			for subcommandName, options := range subcommand {
				if f, ok := funcs[commandName][subcommandName]; ok {
					j, err := f(options)
					if err != nil {
						return nil, err
					}

					playbook.Jobs = append(playbook.Jobs, j)
				} else {
					return nil, errors.New("unsupported command: " + commandName + " " + subcommandName)
				}
			}
		}
	}

	return playbook, nil
}

func (p *Playbook) Run() error {
	for i, j := range p.Jobs {
		log.Println()
		log.Printf("Running job %d/%d ...", i+1, len(p.Jobs))
		if err := j.Run(p.ApiClient, p.UiClient); err != nil {
			return err
		}

		log.Println("Job complete.")
	}
	return nil
}
