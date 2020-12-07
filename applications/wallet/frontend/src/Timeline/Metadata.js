import PropTypes from 'prop-types'

import { colors, spacing, constants, typography } from '../Styles'

import TimebasedMetadataSvg from '../Icons/timebasedMetadata.svg'

import { useLocalStorage } from '../LocalStorage/helpers'
import Button, { VARIANTS } from '../Button'

import { MIN_WIDTH } from '../MetadataCues'

const TimelineMetadata = ({ assetId }) => {
  const [{ isOpen } = {}, dispatch] = useLocalStorage({
    key: `MetadataCues.${assetId}`,
    reducer: (state, action) => ({ ...state, ...action }),
  })

  return (
    <Button
      aria-label={`${isOpen ? 'Close' : 'Open'} Metadata`}
      variant={VARIANTS.ICON}
      style={{
        flexDirection: 'row',
        alignItems: 'flex-end',
        padding: spacing.small,
        ':hover, &.focus-visible:focus': {
          backgroundColor: colors.structure.mattGrey,
          color: isOpen ? colors.key.one : colors.structure.white,
          svg: {
            path: {
              fill: isOpen ? colors.key.one : colors.structure.white,
            },
          },
        },
        fontFamily: typography.family.condensed,
        textTransform: 'uppercase',
        color: isOpen ? colors.key.one : colors.structure.steel,
      }}
      onClick={() =>
        dispatch({
          isOpen: !isOpen,
          originSize: isOpen ? 0 : MIN_WIDTH,
        })
      }
    >
      <TimebasedMetadataSvg
        height={constants.icons.regular}
        color={isOpen ? colors.key.one : colors.structure.steel}
        css={{ marginRight: spacing.small }}
      />
      Metadata
    </Button>
  )
}

TimelineMetadata.propTypes = {
  assetId: PropTypes.string.isRequired,
}

export default TimelineMetadata
