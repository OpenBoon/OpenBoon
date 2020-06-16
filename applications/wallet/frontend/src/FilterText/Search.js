import PropTypes from 'prop-types'

import filterShape from '../Filter/shape'

import HiddenSvg from '../Icons/hidden.svg'
import CrossSvg from '../Icons/cross.svg'
import TextSvg from '../Icons/text.svg'

import { spacing, constants, colors, typography } from '../Styles'

import Button, { VARIANTS } from '../Button'

import { dispatch, ACTIONS } from '../Filters/helpers'

const SVG_SIZE = 20

const FilterTextSearch = ({
  projectId,
  assetId,
  filters,
  filter,
  filter: {
    isDisabled,
    values: { query },
  },
  filterIndex,
}) => {
  return (
    <div
      css={{
        border: constants.borders.transparent,
        borderBottom: constants.borders.tabs,
        ':hover': {
          border: constants.borders.tableRow,
          svg: { opacity: 1 },
        },
        padding: spacing.small,
        paddingLeft: spacing.base,
      }}
    >
      <div
        css={{
          width: '100%',
          display: 'flex',
          alignItems: 'center',
        }}
      >
        <div
          css={{
            display: 'flex',
            paddingLeft: spacing.comfy,
          }}
        >
          <TextSvg css={{ width: SVG_SIZE, color: colors.key.one }} />
        </div>

        <div
          css={{
            flex: 1,
            minWidth: 0,
            width: '100%',
            display: 'flex',
            alignItems: 'center',
          }}
        >
          <span
            css={{
              fontFamily: typography.family.mono,
              fontSize: typography.size.small,
              lineHeight: typography.height.small,
              paddingLeft: spacing.base,
              overflow: 'hidden',
              whiteSpace: 'nowrap',
              textOverflow: 'ellipsis',
            }}
          >
            {query}
          </span>
        </div>

        <Button
          aria-label={`${isDisabled ? 'Enable' : 'Disable'} Filter`}
          variant={VARIANTS.ICON}
          css={{
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center',
            borderRadius: constants.borderRadius.small,
            ':hover, :focus': {
              backgroundColor: colors.structure.smoke,
              svg: {
                opacity: 1,
                color: isDisabled
                  ? colors.signal.canary.strong
                  : colors.structure.white,
              },
            },
          }}
          onClick={() =>
            dispatch({
              action: ACTIONS.UPDATE_FILTER,
              payload: {
                projectId,
                assetId,
                filters,
                updatedFilter: {
                  ...filter,
                  isDisabled: !isDisabled,
                },
                filterIndex,
              },
            })
          }
        >
          <HiddenSvg
            width={SVG_SIZE}
            color={
              isDisabled ? colors.signal.canary.strong : colors.structure.steel
            }
            css={{ opacity: isDisabled ? 1 : 0 }}
          />
        </Button>

        <Button
          aria-label="Delete Filter"
          variant={VARIANTS.ICON}
          css={{
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center',
            borderRadius: constants.borderRadius.small,
            ':hover, :focus': {
              backgroundColor: colors.structure.smoke,
              svg: { opacity: 1, color: colors.structure.white },
            },
          }}
          onClick={() =>
            dispatch({
              action: ACTIONS.DELETE_FILTER,
              payload: {
                projectId,
                assetId,
                filters,
                filterIndex,
              },
            })
          }
        >
          <CrossSvg
            width={SVG_SIZE}
            color={colors.structure.steel}
            css={{ opacity: 0 }}
          />
        </Button>
      </div>
    </div>
  )
}

FilterTextSearch.propTypes = {
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
  filters: PropTypes.arrayOf(PropTypes.shape(filterShape)).isRequired,
  filter: PropTypes.shape(filterShape).isRequired,
  filterIndex: PropTypes.number.isRequired,
}

export default FilterTextSearch
