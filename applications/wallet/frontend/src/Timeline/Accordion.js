import PropTypes from 'prop-types'

import { colors, constants, spacing } from '../Styles'

import ChevronSvg from '../Icons/chevron.svg'

import { ACTIONS } from './reducer'

export const COLOR_TAB_WIDTH = 3

const TimelineAccordion = ({
  color,
  timeline,
  tracks,
  dispatch,
  isOpen,
  children,
}) => {
  return (
    <details
      css={{
        backgroundColor: colors.structure.soot,
        borderBottom: constants.borders.regular.smoke,
      }}
      open={isOpen}
    >
      <summary
        aria-label={timeline}
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
          dispatch({ type: ACTIONS.TOGGLE_OPEN, payload: { timeline } })
        }}
      >
        <div css={{ display: 'flex' }}>
          <div
            css={{
              width: COLOR_TAB_WIDTH,
              backgroundColor: color,
              marginRight: spacing.base,
            }}
          />

          <ChevronSvg
            height={constants.icons.regular}
            css={{
              color,
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
            {timeline}
          </div>

          <div
            css={{
              padding: spacing.base,
            }}
          >{`(${tracks.length})`}</div>
        </div>
      </summary>

      {isOpen && <div>{children}</div>}
    </details>
  )
}

TimelineAccordion.propTypes = {
  color: PropTypes.string.isRequired,
  timeline: PropTypes.string.isRequired,
  tracks: PropTypes.arrayOf(
    PropTypes.shape({
      track: PropTypes.string.isRequired,
      hits: PropTypes.arrayOf(
        PropTypes.shape({
          start: PropTypes.number.isRequired,
          stop: PropTypes.number.isRequired,
        }).isRequired,
      ).isRequired,
    }).isRequired,
  ).isRequired,
  isOpen: PropTypes.bool.isRequired,
  dispatch: PropTypes.func.isRequired,
  children: PropTypes.node.isRequired,
}

export default TimelineAccordion
