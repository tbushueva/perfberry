package cmd

import (
	"strconv"

	"github.com/tbushueva/perfberry/perfberry-cli/api"
	"github.com/tbushueva/perfberry/perfberry-cli/handlers"
	"github.com/tbushueva/perfberry/perfberry-cli/ui"
	"github.com/urfave/cli"
)

func reportsCmd(ac *api.Client, uc *ui.Client) cli.Command {
	return cli.Command{
		Name:  "reports",
		Usage: "Manage reports",
		Subcommands: []cli.Command{
			{
				Name:  "create",
				Usage: "Create report",
				Flags: []cli.Flag{
					&cli.StringFlag{
						Name: "report-file",
						// Aliases: []string{"r"},
						Usage: "path to YAML file with report",
					},
					&cli.StringFlag{
						Name: "output-id-file",
						// Aliases: []string{"i"},
						Usage: "path to file with created report id",
					},
					&cli.StringFlag{
						Name: "output-file",
						// Aliases: []string{"o"},
						Usage: "path to YAML file with created report",
					},
				},
				ArgsUsage: "PROJECT_ID",
				Action: func(c *cli.Context) error {
					projectId, err := strconv.Atoi(c.Args().First())
					if err != nil {
						return err
					}

					return handlers.CreateReport(
						projectId,
						c.String("report-file"),
						c.String("output-id-file"),
						c.String("output-file"),
						ac,
						uc,
					)
				},
			},
		},
	}
}
