import PropTypes from 'prop-types'

import filterShape from './shape'

import { colors, constants } from '../Styles'

import { dispatch, ACTIONS } from '../Filters/helpers'

import HiddenSvg from '../Icons/hidden.svg'
import CrossSvg from '../Icons/cross.svg'

import Button, { VARIANTS } from '../Button'

const SVG_SIZE = 20

const FilterActions = ({
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
        onClick={(event) => {
          event.stopPropagation()

          dispatch({
            action: ACTIONS.UPDATE_FILTER,
            payload: {
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
          width={SVG_SIZE}
          color={
            isDisabled ? colors.signal.canary.strong : colors.structure.steel
          }
          css={{ visibility: isDisabled ? '' : 'hidden' }}
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
        onClick={(event) => {
          event.stopPropagation()

          dispatch({
            action: ACTIONS.DELETE_FILTER,
            payload: {
              projectId,
              assetId,
              filters,
              filterIndex,
            },
          })
        }}
      >
        <CrossSvg
          width={SVG_SIZE}
          color={colors.structure.steel}
          css={{ visibility: 'hidden' }}
        />
      </Button>
    </div>
  )
}

FilterActions.propTypes = {
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
  filters: PropTypes.arrayOf(PropTypes.shape(filterShape)).isRequired,
  filter: PropTypes.shape(filterShape).isRequired,
  filterIndex: PropTypes.number.isRequired,
}

export default FilterActions
