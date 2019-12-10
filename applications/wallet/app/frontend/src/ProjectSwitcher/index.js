import PropTypes from 'prop-types'
import { useState } from 'react'

import DropDown from './DropDown'
import { colors, typography, spacing } from '../Styles'
import ChevronSvg from '../Icons/chevron.svg'

const CHEVRON_WIDTH = 20

const ProjectSwitcher = ({ projects, setSelectedProject }) => {
  const [isDropDownOpen, setDropDownOpen] = useState(false)

  const selectedProject = projects.find(project => project.selected)

  return (
    <div
      css={{
        display: 'flex',
        position: 'relative',
        paddingLeft: spacing.normal,
        height: '100%',
      }}>
      <button
        type="button"
        onClick={() => setDropDownOpen(!isDropDownOpen)}
        css={{
          display: 'flex',
          alignItems: 'center',
          fontSize: typography.size.hecto,
          border: 0,
          margin: 0,
          padding: 0,
          color: colors.primary,
          backgroundColor: colors.grey1,
          ':hover': {
            cursor: 'pointer',
          },
        }}>
        {selectedProject.name}
        <ChevronSvg
          width={CHEVRON_WIDTH}
          css={{
            marginLeft: spacing.base,
            transform: `${isDropDownOpen ? 'rotate(-180deg)' : ''}`,
          }}
        />
      </button>
      {isDropDownOpen && (
        <DropDown
          projects={projects.filter(project => !project.selected)}
          onSelect={({ project }) => {
            setDropDownOpen(false)
            setSelectedProject(project)
          }}
        />
      )}
    </div>
  )
}

ProjectSwitcher.propTypes = {
  projects: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.string.isRequired,
      name: PropTypes.string.isRequired,
    }),
  ).isRequired,
  setSelectedProject: PropTypes.func.isRequired,
}

export default ProjectSwitcher
