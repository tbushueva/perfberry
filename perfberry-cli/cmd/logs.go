package cmd

import (
	"strconv"

	"github.com/tbushueva/perfberry/perfberry-cli/api"
	"github.com/tbushueva/perfberry/perfberry-cli/handlers"
	"github.com/tbushueva/perfberry/perfberry-cli/ui"
	"github.com/urfave/cli"
)

func logsCmd(ac *api.Client, uc *ui.Client) cli.Command {
	return cli.Command{
		Name:  "logs",
		Usage: "Manage logs from your performance tools",
		Subcommands: []cli.Command{
			{
				Name:  "upload",
				Usage: "Upload log and parse to report",
				Flags: []cli.Flag{
					&cli.StringFlag{
						Name: "dir",
						// Aliases: []string{"d"},
						Value: ".",
						Usage: "directory to recursively find logs to upload",
					},
					&cli.BoolFlag{
						Name: "extended",
						// Aliases: []string{"e"},
						Usage: "extended mode for parsing log",
					},
					&cli.IntFlag{
						Name: "report-id",
						// Aliases: []string{"i"},
						Usage: "append build to existed report",
					},
					&cli.StringFlag{
						Name: "report-file",
						// Aliases: []string{"r"},
						Usage: "path to YAML file with report",
					},
					&cli.StringFlag{
						Name: "build-file",
						// Aliases: []string{"b"},
						Usage: "path to YAML file with build",
					},
					&cli.StringFlag{
						Name: "assertions-file",
						// Aliases: []string{"a"},
						Usage: "path to YAML file with assertions",
					},
					&cli.StringFlag{
						Name: "output-file",
						// Aliases: []string{"o"},
						Usage: "path to YAML file with created report",
					},
					&cli.BoolFlag{
						Name: "follow-status",
						// Aliases: []string{"s"},
						Usage: "follow report status, if assertions fails exit with code 1",
					},
				},
				ArgsUsage: "LOG_TYPE PROJECT_ID",
				Action: func(c *cli.Context) error {
					projectId, err := strconv.Atoi(c.Args().Get(1))
					if err != nil {
						return err
					}

					return handlers.UploadLog(
						projectId,
						c.Args().First(),
						c.Bool("extended"),
						c.Int("report-id"),
						c.String("dir"),
						c.String("report-file"),
						c.String("build-file"),
						c.String("assertions-file"),
						c.String("output-file"),
						c.Bool("follow-status"),
						ac,
						uc,
					)
				},
			},
		},
	}
}
