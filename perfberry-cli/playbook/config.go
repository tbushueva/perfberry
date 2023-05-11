package playbook

import (
	"github.com/tbushueva/perfberry/perfberry-cli/helpers"
	"github.com/go-yaml/yaml"
)

type JobConfig map[string]map[string]interface{}

type PlaybookConfig struct {
	Version int         `yaml:"version"`
	Jobs    []JobConfig `yaml:"jobs"`
}

func NewPlaybookConfigFromYAML(data []byte) (p *PlaybookConfig, err error) {
	err = yaml.Unmarshal(data, &p)
	return
}

func NewPlaybookConfigFromFile(path string) (p *PlaybookConfig, err error) {
	data, err := helpers.ProccessTemplateFile(path)
	if err != nil {
		return
	}

	return NewPlaybookConfigFromYAML(data)
}
