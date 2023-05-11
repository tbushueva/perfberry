package cmd

import (
	"log"

	"github.com/tbushueva/perfberry/perfberry-cli/api"
	"github.com/tbushueva/perfberry/perfberry-cli/playbook"
	"github.com/tbushueva/perfberry/perfberry-cli/ui"
	"github.com/urfave/cli"
)

func run(path string, ac *api.Client, uc *ui.Client) error {
	log.Println("Reading playbook from", path, "...")
	config, err := playbook.NewPlaybookConfigFromFile(path)
	if err != nil {
		return err
	}

	log.Println("Initializing playbook ...")
	pb, err := playbook.NewPlaybook(config, ac, uc)
	if err != nil {
		return err
	}

	log.Println("Running playbook ...")
	err = pb.Run()
	if err != nil {
		return err
	}

	log.Println()
	log.Println("Playbook complete.")
	return nil
}

func playbookCmd(ac *api.Client, uc *ui.Client) cli.Command {
	return cli.Command{
		Name:  "playbooks",
		Usage: "Manage playbooks",
		Subcommands: []cli.Command{{
			Name:      "run",
			Usage:     "Run playbook",
			ArgsUsage: "FILE",
			Action: func(c *cli.Context) error {
				return run(c.Args().First(), ac, uc)
			},
		}},
	}
}
