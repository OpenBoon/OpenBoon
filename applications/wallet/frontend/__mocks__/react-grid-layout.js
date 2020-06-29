import { createElement } from 'react'

export const Responsive = ({ children, ...rest }) =>
  createElement('ResponsiveGridLayout', rest, children)
