// src/DropdownMenu.types.ts
import { ViewStyle } from "react-native";

export type IconSpec = {
  resource?: string;
  base64?: string;
};

export type MenuItemSpec = {
  id?: string;
  title: string;
  icon?: IconSpec;
  enabled?: boolean;
  items?: MenuItemSpec[];
  // Add onSelect for component usage
  onSelect?: (info: { id?: string; title: string; path: string[] }) => void;
};

export type MenuGroupSpec = {
  items: MenuItemSpec[];
};

export type MenuSpec = {
  groups: MenuGroupSpec[];
  header?: string;
  backgroundColor?: string;
  cornerRadius?: number;
};

export type ShowOptions = {
  anchorTag: number;
  menu: MenuSpec;
};

export type OnItemSelectedEvent = {
  id?: string;
  title: string;
  path: string[];
};

// Component types
export type DropdownMenuHandle = {
  show: () => void;
};

export type DropdownMenuProps = {
  groups: MenuGroupSpec[];
  header?: string;
  backgroundColor?: string;
  cornerRadius?: number;
  onSelect?: (info: { id?: string; title: string; path: string[] }) => void;
  trigger?: "press" | "longPress" | "none";
  anchorTag?: number | null;
  wrapperStyle?: ViewStyle;
  children?: React.ReactNode;
  disabled?: boolean;
};
