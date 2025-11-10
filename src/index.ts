// src/index.ts
// Export the React component as default
export { default } from "./DropdownMenu.component";

// Export all types
export * from "./DropdownMenu.types";

// Export native module functions for advanced usage
export {
  showMenu,
  addOnItemSelectedListener,
  type ListenerSubscription,
} from "./DropdownMenuModule";
