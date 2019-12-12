import { createElement } from 'react'

const Projects = ({ children, ...rest }) =>
  createElement('Projects', rest, children(rest))

export default Projects
