const BREAKPOINTS = ['sm', 'md', 'lg']

const MIN_COL_WIDTH = 350

export const MIN_ROW_HEIGHT = 50

export const breakpoints = BREAKPOINTS.reduce(
  (acc, bp, index) => ({ ...acc, [bp]: MIN_COL_WIDTH * (index + 1) }),
  {},
)

export const cols = BREAKPOINTS.reduce(
  (acc, bp, index) => ({ ...acc, [bp]: index + 1 }),
  {},
)

export const setAllLayouts = ({ setLayouts }) => (_, allLayouts) => {
  const value = Object.entries(allLayouts).reduce(
    (acc, [bp, values]) => ({
      ...acc,
      [bp]: values.map((v) => ({ ...v, h: v.h > 4 ? v.h : 4, minH: 4 })),
    }),
    {},
  )

  setLayouts({ value })
}
