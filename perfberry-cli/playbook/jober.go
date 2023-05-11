package playbook

import (
	"github.com/tbushueva/perfberry/perfberry-cli/api"
	"github.com/tbushueva/perfberry/perfberry-cli/ui"
)

type Jober interface {
	Run(ac *api.Client, uc *ui.Client) error
}
