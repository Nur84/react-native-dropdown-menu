// src/DropdownMenuModule.ts
import { requireNativeModule } from "expo-modules-core";
import { Platform } from "react-native";
import type { ShowOptions, OnItemSelectedEvent } from "./DropdownMenu.types";

const isWeb = Platform.OS === "web";

const NativeModule: any = isWeb
  ? {
      showMenu: async () => {},
      addListener: (_event: string, _listener: any) => ({ remove() {} }),
    }
  : requireNativeModule<any>("DropdownMenu");

export async function showMenu(options: ShowOptions): Promise<void> {
  await NativeModule.showMenu(options.anchorTag, options.menu);
}

export type ListenerSubscription = { remove: () => void };

export function addOnItemSelectedListener(
  listener: (event: OnItemSelectedEvent) => void
): ListenerSubscription {
  return NativeModule.addListener("onItemSelected", listener);
}

export default NativeModule;
