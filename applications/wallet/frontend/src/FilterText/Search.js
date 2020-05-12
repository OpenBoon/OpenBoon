import PropTypes from 'prop-types'

import filterShape from '../Filter/shape'

import HiddenSvg from '../Icons/hidden.svg'
import CrossSvg from '../Icons/cross.svg'
import SearchSvg from '../Icons/search.svg'

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
          svg: {
            visibility: 'visible',
          },
        },
        padding: spacing.moderate,
      }}
    >
      <div
        css={{
          display: 'flex',
          justifyContent: 'space-between',
          padding: 1,
        }}
      >
        <div
          css={{
            display: 'flex',
            alignItems: 'center',
            color: 'white',
            height: spacing.large,
            width: '100%',
            paddingLeft: spacing.spacious,
            paddingRight: spacing.comfy,
          }}
        >
          <div css={{ display: 'flex', paddingRight: spacing.normal }}>
            <SearchSvg css={{ width: 14, color: colors.key.one }} />
          </div>
          <div
            title={query}
            css={{
              fontFamily: 'Roboto Mono',
              fontSize: typography.size.small,
              lineHeight: typography.height.small,
              overflow: 'hidden',
              textOverflow: 'ellipsis',
              whiteSpace: 'nowrap',
            }}
          >
            {query}
          </div>
        </div>
        <Button
          aria-label={`${isDisabled ? 'Enable' : 'Disable'} Filter`}
          variant={VARIANTS.ICON}
          css={{
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center',
            borderRadius: constants.borderRadius.small,
            ':hover': {
              backgroundColor: colors.structure.smoke,
              svg: {
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
            css={{
              visibility: isDisabled ? '' : 'hidden',
            }}
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
            ':hover': {
              backgroundColor: colors.structure.smoke,
              svg: { color: colors.structure.white },
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
            css={{
              visibility: 'hidden',
            }}
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
