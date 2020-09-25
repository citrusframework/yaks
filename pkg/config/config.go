package config

import (
	"os"

	"github.com/citrusframework/yaks/pkg/util/defaults"
)

func GetTestBaseImage() string {
	customEnv := os.Getenv("TEST_BASE_IMAGE")
	if customEnv != "" {
		return customEnv
	}
	return getDefaultTestBaseImage()
}

func getDefaultTestBaseImage() string {
	return "docker.io/citrusframework/yaks:" + defaults.Version
}
