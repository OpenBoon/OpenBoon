import { createElement } from 'react'

export const ListboxInput = ({ children, ...rest }) =>
  createElement('ListboxInput', rest, children)

export const ListboxButton = ({ children, ...rest }) =>
  createElement('ListboxButton', rest, children)

export const ListboxList = ({ children, ...rest }) =>
  createElement('ListboxList', rest, children)

export const ListboxPopover = ({ children, ...rest }) =>
  createElement('ListboxPopover', rest, children)

export const ListboxOption = ({ children, ...rest }) =>
  createElement('ListboxOption', rest, children)
