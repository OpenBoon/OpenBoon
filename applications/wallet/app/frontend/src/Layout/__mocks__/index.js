import { createElement } from 'react'

const Layout = ({ children, ...rest }) =>
  createElement('Layout', rest, children(rest))

export default Layout
