import PropTypes from 'prop-types'

import { colors, spacing, constants, typography, zIndex } from '../Styles'

const TOP = 24

const ProjectSwitcherDropDown = ({ projects, onSelect }) => {
  return (
    <div
      css={{
        position: 'absolute',
        zIndex: zIndex.reset,
        top: TOP,
        left: 0,
        minWidth: '100%',
        borderRadius: constants.borderRadius.small,
        paddingTop: spacing.base,
        paddingBottom: spacing.base,
        backgroundColor: colors.grey6,
        boxShadow: constants.boxShadows.dropdown,
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
            color: colors.grey7,
            backgroundColor: colors.grey6,
            fontSize: typography.size.hecto,
            border: 0,
            width: '100%',
            textAlign: 'left',
            ':hover': {
              backgroundColor: colors.rocks.iron,
              color: colors.rocks.white,
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
  projects: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.string.isRequired,
      name: PropTypes.string.isRequired,
    }),
  ).isRequired,
  onSelect: PropTypes.func.isRequired,
}

export default ProjectSwitcherDropDown
