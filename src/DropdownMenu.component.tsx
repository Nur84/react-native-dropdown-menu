// src/DropdownMenu.component.tsx
import React, {
  forwardRef,
  useCallback,
  useImperativeHandle,
  useMemo,
  useRef,
  useEffect,
} from "react";
import { Pressable, findNodeHandle } from "react-native";
import type {
  OnItemSelectedEvent,
  MenuSpec,
  MenuItemSpec,
  MenuGroupSpec,
  DropdownMenuHandle,
  DropdownMenuProps,
} from "./DropdownMenu.types";
import {
  addOnItemSelectedListener,
  showMenu,
  type ListenerSubscription,
} from "./DropdownMenuModule";

type HandlerMap = Map<
  string,
  (info: { id?: string; title: string; path: string[] }) => void
>;

const idKey = (id: string) => `id:${id}`;
const pathKey = (path: string[]) => `path:${path.join(">")}`;

function toMenuSpec(
  groups: MenuGroupSpec[],
  header?: string,
  backgroundColor?: string,
  cornerRadius?: number
): { menu: MenuSpec; handlers: HandlerMap } {
  const handlers: HandlerMap = new Map();

  function walk(list: MenuItemSpec[], path: string[]): MenuItemSpec[] {
    return list.map((it) => {
      const base: MenuItemSpec = {
        id: it.id,
        title: it.title,
        icon: it.icon,
        enabled: it.enabled,
      };

      if (it.items && it.items.length > 0) {
        return {
          ...base,
          items: walk(it.items, [...path, it.title]),
        };
      }

      const fullPath = [...path, it.title];
      if (it.onSelect) {
        if (it.id) handlers.set(idKey(it.id), it.onSelect);
        handlers.set(pathKey(fullPath), it.onSelect);
      }

      return base;
    });
  }

  const menu: MenuSpec = {
    groups: groups.map((group) => ({
      items: walk(group.items, []),
    })),
    header,
    backgroundColor,
    cornerRadius,
  };

  return { menu, handlers };
}

export const DropdownMenu = forwardRef<DropdownMenuHandle, DropdownMenuProps>(
  function DropdownMenu(props, ref) {
    const {
      groups,
      header,
      backgroundColor,
      cornerRadius,
      onSelect,
      trigger = "press",
      anchorTag,
      wrapperStyle,
      children,
      disabled,
    } = props;

    const wrapperRef = useRef<React.ComponentRef<typeof Pressable>>(null);
    const activeSubRef = useRef<ListenerSubscription | null>(null);

    const cleanupActiveSub = useCallback(() => {
      if (activeSubRef.current) {
        try {
          activeSubRef.current.remove();
        } catch {}
        activeSubRef.current = null;
      }
    }, []);

    useEffect(() => {
      return () => {
        cleanupActiveSub();
      };
    }, [cleanupActiveSub]);

    const { menu, handlers } = useMemo(
      () => toMenuSpec(groups, header, backgroundColor, cornerRadius),
      [groups, header, backgroundColor, cornerRadius]
    );

    const routeSelection = useCallback(
      (e: OnItemSelectedEvent) => {
        if (e.id && handlers.has(idKey(e.id))) {
          handlers.get(idKey(e.id))?.(e);
          return;
        }
        const key = pathKey(e.path);
        if (handlers.has(key)) {
          handlers.get(key)?.(e);
          return;
        }
        onSelect?.(e);
      },
      [handlers, onSelect]
    );

    const getAnchorTag = useCallback((): number | null => {
      if (typeof anchorTag === "number") return anchorTag;
      const node = findNodeHandle(wrapperRef.current) as number | null;
      return node ?? null;
    }, [anchorTag]);

    const showFunctionBased = useCallback(() => {
      const tag = getAnchorTag();
      if (typeof tag !== "number") return;

      const nativeMenu = JSON.parse(JSON.stringify(menu)) as MenuSpec;

      cleanupActiveSub();
      const selSub = addOnItemSelectedListener((e: OnItemSelectedEvent) => {
        cleanupActiveSub();
        routeSelection(e);
      });
      activeSubRef.current = selSub;

      void showMenu({ anchorTag: tag, menu: nativeMenu });
    }, [getAnchorTag, menu, routeSelection, cleanupActiveSub]);

    const show = useCallback(() => {
      showFunctionBased();
    }, [showFunctionBased]);

    useImperativeHandle(ref, () => ({ show }), [show]);

    const triggerProps: Partial<React.ComponentProps<typeof Pressable>> = {};
    if (!disabled) {
      if (trigger === "press") triggerProps.onPress = show;
      else if (trigger === "longPress") triggerProps.onLongPress = show;
    }

    return (
      <>
        {children ? (
          <Pressable
            ref={wrapperRef}
            style={[wrapperStyle, { borderRadius: "100%", overflow: "hidden" }]}
            onLongPress={() => {}}
            android_ripple={{
              color: "rgba(100, 100, 100, .1)",
              borderless: false,
              foreground: true,
            }}
            {...triggerProps}
          >
            {children}
          </Pressable>
        ) : null}
      </>
    );
  }
);

export default DropdownMenu;
