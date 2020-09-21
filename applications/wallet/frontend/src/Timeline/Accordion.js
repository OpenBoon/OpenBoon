import PropTypes from 'prop-types'

import { colors, constants, spacing } from '../Styles'

import ChevronSvg from '../Icons/chevron.svg'

export const COLOR_TAB_WIDTH = 3

const TimelineAccordion = ({
  moduleColor,
  name,
  predictions,
  dispatch,
  isOpen,
  children,
}) => {
  return (
    <details
      css={{
        backgroundColor: colors.structure.soot,
        borderBottom: constants.borders.regular.smoke,
        borderRight: constants.borders.regular.smoke,
      }}
      open={isOpen}
    >
      <summary
        aria-label={name}
        css={{
          listStyleType: 'none',
          '::-webkit-details-marker': { display: 'none' },
          ':hover': {
            cursor: 'pointer',
            backgroundColor: colors.structure.mattGrey,
          },
          backgroundColor: colors.structure.soot,
        }}
        onClick={(event) => {
          event.preventDefault()
          dispatch({ [name]: !isOpen })
        }}
      >
        <div css={{ display: 'flex' }}>
          <div
            css={{
              width: COLOR_TAB_WIDTH,
              backgroundColor: moduleColor,
              marginRight: spacing.base,
            }}
          />

          <ChevronSvg
            height={constants.icons.regular}
            css={{
              color: moduleColor,
              transform: isOpen ? '' : 'rotate(-90deg)',
              alignSelf: 'center',
            }}
          />

          <div
            css={{
              flex: 1,
              overflow: 'hidden',
              textOverflow: 'ellipsis',
              whiteSpace: 'nowrap',
              padding: spacing.base,
              paddingRight: 0,
            }}
          >
            {name}
          </div>

          <div
            css={{
              padding: spacing.base,
            }}
          >{`(${predictions.length})`}</div>
        </div>
      </summary>

      {isOpen && <div>{children}</div>}
    </details>
  )
}

TimelineAccordion.propTypes = {
  moduleColor: PropTypes.string.isRequired,
  name: PropTypes.string.isRequired,
  predictions: PropTypes.arrayOf(
    PropTypes.shape({
      label: PropTypes.string.isRequired,
      count: PropTypes.number.isRequired,
    }),
  ).isRequired,
  isOpen: PropTypes.bool.isRequired,
  dispatch: PropTypes.func.isRequired,
  children: PropTypes.node.isRequired,
}

export default TimelineAccordion
