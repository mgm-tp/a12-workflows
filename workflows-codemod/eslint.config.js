/**
 * (c) copyright 2012-2026 mgm technology partners GmbH.
 * This software, the underlying source code and other artifacts are protected by copyright.
 * All rights, in particular the right to use, reproduce, publish and edit are reserved.
 * A simple right of use (license) can be acquired for use, duplication, publication, editing etc..
 * Requests for this can be made at A12-license@mgm-tp.com or other official channels of the copyright holder.
 */

import notice from "eslint-plugin-notice";
import {reactRecommended} from "@com.mgmtp.a12.devtools/eslint-config";


export default [
    ...reactRecommended,
	{
		plugins: {
			notice
		},

		rules: {
			"@typescript-eslint/no-empty-object-type": "warn",
			"@typescript-eslint/no-unsafe-function-type": "warn",
			"@typescript-eslint/no-explicit-any": "warn",
			"@typescript-eslint/no-namespace": "off",
			"@typescript-eslint/no-empty-function": "warn",
			"@typescript-eslint/ban-ts-comment": "warn",
			"@typescript-eslint/no-unused-vars": [
				"warn",
				{
					ignoreRestSiblings: true
				}
			],

			curly: "error",
			"no-inner-declarations": "off",

			"react/display-name": "off",
			"react/prop-types": "off",
			"react/react-in-jsx-scope": "off",
			"react-hooks/rules-of-hooks": "error",
			"react-hooks/exhaustive-deps": "error",

			"import/no-extraneous-dependencies": "error",

			"notice/notice": [
				"error",
				{
					templateFile: "./header.js",
					onNonMatchingHeader: "replace",
                    chars: 5000
				}
			]
		}
	},

	{
		files: ["./**/*.ts", "./**/*.tsx", "./**/*.js", "./**/*.jsx"]
	},

	{
		ignores: [
			"prettier.config.js",
			"eslint.config.js",
			"node_modules",
			"**/*.js",
			"**/*.d.ts",
			"./**/node_modules",
			"target",
			"lib",
			"build"
		]
	}
];
