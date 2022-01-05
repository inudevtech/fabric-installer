/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.fabricmc.installer.client;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

import net.fabricmc.installer.LoaderVersion;
import net.fabricmc.installer.util.InstallerProgress;
import net.fabricmc.installer.util.Reference;
import net.fabricmc.installer.util.Utils;

public class ClientInstaller {
	public static String install(Path mcDir, Path gameDir, String gameVersion, LoaderVersion loaderVersion, InstallerProgress progress, Boolean optifine) throws IOException {
		System.out.println(gameDir);
		System.out.println("Installing " + gameVersion + " with fabric " + loaderVersion.name);

		String profileName = String.format("%s-%s-%s", Reference.LOADER_NAME, loaderVersion.name, gameVersion);

		Path versionsDir = mcDir.resolve("versions");
		Path profileDir = versionsDir.resolve(profileName);
		Path profileJson = profileDir.resolve(profileName + ".json");

		if (!Files.exists(profileDir)) {
			Files.createDirectories(profileDir);
		}

		/*

		This is a fun meme

		The vanilla launcher assumes the profile name is the same name as a maven artifact, how ever our profile name is a combination of 2
		(mappings and loader). The launcher will also accept any jar with the same name as the profile, it doesnt care if its empty

		 */
		Path dummyJar = profileDir.resolve(profileName + ".jar");
		Files.deleteIfExists(dummyJar);
		Files.createFile(dummyJar);

		URL profileUrl = new URL(Reference.getMetaServerEndpoint(String.format("v2/versions/loader/%s/%s/profile/json", gameVersion, loaderVersion.name)));
		Utils.downloadFile(profileUrl, profileJson);

		/*
		ここから魔改造ライン
		 */
		Path modsDir = gameDir.resolve("mods");

		if (!Files.exists(modsDir)) {
			Files.createDirectories(modsDir);
		} else {
			File[] listFiles = Objects.requireNonNull(modsDir.toFile().listFiles());

			if (listFiles.length != 0) {
				Path oldFileDir = modsDir.resolve("old_files");
				if (!Files.exists(oldFileDir)) Files.createDirectories(oldFileDir);

				for (File i : listFiles) {
					if (!i.toPath().equals(oldFileDir)) {
						Files.move(i.toPath(), oldFileDir.resolve(i.getName()), StandardCopyOption.REPLACE_EXISTING);
					}
				}
			}
		}

		Path APIJar = modsDir.resolve("fabric-api.jar");
		Path identityJar = modsDir.resolve("identity.jar");
		URL APIUrl = new URL("https://github.com/FabricMC/fabric/releases/download/0.45.1%2B1.17/fabric-api-0.45.1+1.17.jar");
		Utils.downloadFile(APIUrl, APIJar);
		URL identityUrl = new URL("https://github.com/Draylar/identity/releases/download/1.15.2-1.17.1-fabric/identity-1.15.2-1.17.1.jar");
		Utils.downloadFile(identityUrl, identityJar);

		if (optifine) {
			Path optifineJar = modsDir.resolve("optifine.jar");
			URL optifineUrl = new URL("https://data.arainu.world/files/OptiFine_1.17.1_HD_U_H1.jar");
			Utils.downloadFile(optifineUrl, optifineJar);
			Path optifabricJar = modsDir.resolve("optifine.jar");
			URL optifabricURL = new URL("https://data.arainu.world/files/optifabric-1.12.10.jar");
			Utils.downloadFile(optifabricURL, optifabricJar);
		}

		progress.updateProgress(Utils.BUNDLE.getString("progress.done"));

		return profileName;
	}
}
