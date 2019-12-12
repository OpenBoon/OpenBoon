import { createElement } from 'react'

const Projects = ({ children, ...rest }) =>
  createElement('Projects', rest, children(rest))

export const projects = [
  {
    id: '1',
    name: 'Zorroa ABC',
  },
  {
    id: '2',
    name: 'Zorroa EasyAs123',
  },
  {
    id: '3',
    name: 'Zorroa JustYouAndMe',
  },
]

export default Projects
