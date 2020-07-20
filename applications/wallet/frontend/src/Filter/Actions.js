import PropTypes from 'prop-types'

import filterShape from './shape'

import { colors, constants, spacing } from '../Styles'

import { dispatch, ACTIONS } from '../Filters/helpers'

import HiddenSvg from '../Icons/hidden.svg'
import CrossSvg from '../Icons/cross.svg'

import Button, { VARIANTS } from '../Button'

const ICON_SIZE = 20

const FilterActions = ({
  pathname,
  projectId,
  assetId,
  filters,
  filter,
  filter: { isDisabled },
  filterIndex,
}) => {
  return (
    <div css={{ display: 'flex', alignItems: 'center' }}>
      <Button
        aria-label={`${isDisabled ? 'Enable' : 'Disable'} Filter`}
        variant={VARIANTS.ICON}
        css={{
          display: 'flex',
          padding: spacing.small,
          justifyContent: 'center',
          alignItems: 'center',
          borderRadius: constants.borderRadius.small,
          ':hover, &.focus-visible:focus': {
            backgroundColor: colors.structure.smoke,
            svg: {
              color: isDisabled
                ? colors.signal.canary.strong
                : colors.structure.white,
            },
          },
        }}
        onClick={(event) => {
          event.stopPropagation()

          dispatch({
            type: ACTIONS.UPDATE_FILTER,
            payload: {
              pathname,
              projectId,
              assetId,
              filters,
              updatedFilter: { ...filter, isDisabled: !isDisabled },
              filterIndex,
            },
          })
        }}
      >
        <HiddenSvg
          height={ICON_SIZE}
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
          padding: spacing.small,
          justifyContent: 'center',
          alignItems: 'center',
          borderRadius: constants.borderRadius.small,
          ':hover, &.focus-visible:focus': {
            backgroundColor: colors.structure.smoke,
          },
        }}
        onClick={(event) => {
          event.stopPropagation()

          dispatch({
            type: ACTIONS.DELETE_FILTER,
            payload: {
              pathname,
              projectId,
              assetId,
              filters,
              filterIndex,
            },
          })
        }}
      >
        <CrossSvg
          height={ICON_SIZE}
          color={colors.structure.steel}
          css={{ opacity: 0 }}
        />
      </Button>
    </div>
  )
}

FilterActions.propTypes = {
  pathname: PropTypes.string.isRequired,
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
  filters: PropTypes.arrayOf(PropTypes.shape(filterShape)).isRequired,
  filter: PropTypes.shape(filterShape).isRequired,
  filterIndex: PropTypes.number.isRequired,
}

export default FilterActions
