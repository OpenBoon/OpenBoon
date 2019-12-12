import { createElement } from 'react'

const Projects = ({ children, ...rest }) =>
  createElement('Projects', rest, children(rest))

export const projects = [
  {
    url: 'http://localhost:3000/api/v1/projects/1',
    name: 'Zorroa ABC',
  },
  {
    url: 'http://localhost:3000/api/v1/projects/2',
    name: 'Zorroa EasyAs123',
  },
  {
    url: 'http://localhost:3000/api/v1/projects/3',
    name: 'Zorroa JustYouAndMe',
  },
]

export default Projects
