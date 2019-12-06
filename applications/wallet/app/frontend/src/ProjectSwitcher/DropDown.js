import PropTypes from 'prop-types'

import { colors, spacing, constants, zIndex } from '../Styles'

const ProjectSwitcherDropDown = ({ projects, onSelect }) => {
  return (
    <div
      css={{
        position: 'absolute',
        zIndex: zIndex.reset,
        top: spacing.comfy,
        left: 0,
        borderRadius: constants.borderRadius.small,
        paddingTop: spacing.base,
        paddingBottom: spacing.base,
        backgroundColor: colors.rocks.charcoal,
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
            color: colors.rocks.pebble,
            backgroundColor: colors.rocks.charcoal,
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
  onSelect: PropTypes.func.isRequired,
  projects: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.string.isRequired,
      name: PropTypes.string.isRequired,
    }),
  ).isRequired,
}

export default ProjectSwitcherDropDown
