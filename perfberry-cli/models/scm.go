package models

type VcsInfo struct {
	Reference string  `json:"reference" yaml:"reference"`
	Revision  string  `json:"revision" yaml:"revision"`
	Title     *string `json:"title,omitempty" yaml:"title,omitempty"`
}

type ScmInfo struct {
	Vcs        *VcsInfo          `json:"vcs,omitempty" yaml:"vcs,omitempty"`
	Parameters map[string]string `json:"parameters" yaml:"parameters"`
}
