import PropTypes from 'prop-types'

import { colors, constants, spacing } from '../Styles'

import { useLocalStorageState } from '../LocalStorage/helpers'

import ChevronSvg from '../Icons/chevron.svg'

export const COLOR_TAB_WIDTH = 3

const TimelineAccordion = ({
  cacheKey,
  moduleColor,
  module: { name, predictions },
  children,
}) => {
  const [isOpen, setOpen] = useLocalStorageState({
    key: cacheKey,
    initialValue: false,
  })

  return (
    <details
      css={{
        backgroundColor: colors.structure.soot,
        borderBottom: constants.borders.regular.smoke,
      }}
      open={isOpen}
      onToggle={({ target: { open } }) => setOpen({ value: open })}
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
      >
        <div css={{ display: 'flex' }}>
          <div
            css={{
              width: COLOR_TAB_WIDTH,
              backgroundColor: moduleColor,
              marginRight: spacing.moderate,
            }}
          />{' '}
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
      <div>{children}</div>
    </details>
  )
}

TimelineAccordion.propTypes = {
  moduleColor: PropTypes.string.isRequired,
  module: PropTypes.shape({
    name: PropTypes.string.isRequired,
    predictions: PropTypes.arrayOf(
      PropTypes.shape({
        label: PropTypes.string.isRequired,
        count: PropTypes.number.isRequired,
      }),
    ).isRequired,
  }).isRequired,
  cacheKey: PropTypes.string.isRequired,
  children: PropTypes.node.isRequired,
}

export default TimelineAccordion
