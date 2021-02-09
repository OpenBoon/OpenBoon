import { createElement } from 'react'

const Tippy = ({ children, render, ...rest }) =>
  createElement('Tippy', rest, createElement('Tooltip', {}, render()), children)

export default Tippy
