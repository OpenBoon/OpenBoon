import PropTypes from 'prop-types'

import { spacing, constants, zIndex } from '../Styles'

const HEIGHT = 40

const ProjectSwitcherDropDown = ({ projects, onSelect }) => {
  return (
    <div
      css={{
        position: 'absolute',
        zIndex: zIndex.reset,
        top: HEIGHT - spacing.base,
        left: 0,
        borderRadius: constants.borderRadius.small,
        boxShadow: constants.boxShadows.menu,
        paddingTop: spacing.base,
        paddingBottom: spacing.base,
        backgroundColor: 'red',
        width: 'auto',
      }}>
      {projects.map(({ id, name }) => (
        <button
          type="button"
          key={id}
          onClick={() => onSelect({ id, name })}
          css={{
            paddingTop: spacing.base,
            paddingRight: spacing.giant,
            paddingBottom: spacing.base,
            paddingLeft: spacing.normal,
            color: 'white',
            ':hover': {
              backgroundColor: 'white',
              color: 'red',
              cursor: 'pointer',
            },
          }}>
          {name}
        </button>
      ))}
    </div>
  )
}

ProjectSwitcherDropDown.propTypes = {
  onSelect: PropTypes.func.isRequired,
  projects: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.string.isRequired,
      name: PropTypes.string.isRequired,
    }),
  ).isRequired,
}

export default ProjectSwitcherDropDown
