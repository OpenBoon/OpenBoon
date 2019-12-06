import { useState, useEffect } from 'react'

import projects from './__mocks__/projects'

import DropDown from './DropDown'
import { colors, typography, spacing } from '../Styles'
import ChevronSvg from './chevron.svg'

const CHEVRON_WIDTH = 20

const ProjectSwitcher = () => {
  const [isDropDownOpen, setDropDownOpen] = useState(false)
  const [selectedProject, setSelectedProject] = useState({})

  // const { data: { results = [] } = {} } = useSWR('/api/v1/projects/')
  const onSelect = project => {
    setDropDownOpen(false)
    setSelectedProject(project)
  }

  useEffect(() => {
    setSelectedProject(projects.list[0])
  }, [])

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
          projects={projects.list.filter(
            project => project.id !== selectedProject.id,
          )}
          onSelect={onSelect}
        />
      )}
    </div>
  )
}

export default ProjectSwitcher
