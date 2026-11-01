// mexa-transform：把订阅 config.yaml 按覆写清单（YAML/JS）依次套用后输出。
// 与 Android 的 mishka_core/overrides 共用同一份 Go 实现，避免两套语义漂移。
package main

import (
	"flag"
	"fmt"
	"os"

	"mishka_core/overrides"
)

func main() {
	base := flag.String("base", "", "input config.yaml")
	out := flag.String("out", "", "output config.yaml")
	specs := flag.String("specs", "", "transform specs json")
	flag.Parse()
	if *base == "" || *out == "" || *specs == "" {
		fmt.Fprintln(os.Stderr, "usage: mexa-transform -base config.yaml -out runtime.yaml -specs specs.json")
		os.Exit(2)
	}
	data, err := os.ReadFile(*base)
	if err != nil {
		fmt.Fprintln(os.Stderr, "read base:", err)
		os.Exit(1)
	}
	transform, err := overrides.LoadTransform(*specs)
	if err != nil {
		fmt.Fprintln(os.Stderr, "load specs:", err)
		os.Exit(1)
	}
	result, err := transform.Apply(data)
	if err != nil {
		fmt.Fprintln(os.Stderr, "apply:", err)
		os.Exit(1)
	}
	if err := os.WriteFile(*out, result, 0o600); err != nil {
		fmt.Fprintln(os.Stderr, "write:", err)
		os.Exit(1)
	}
}
