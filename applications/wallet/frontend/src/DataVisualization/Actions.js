import PropTypes from 'prop-types'

import { constants, spacing } from '../Styles'

import Button, { VARIANTS } from '../Button'

import PlusSvg from '../Icons/plus.svg'

import { ACTIONS } from './reducer'

const DataVisualizationActions = ({ dispatch, setIsCreating, setLayouts }) => {
  return (
    <div css={{ display: 'flex', paddingBottom: spacing.normal }}>
      <Button
        aria-label="Add Chart"
        variant={VARIANTS.SECONDARY_SMALL}
        onClick={() => {
          setIsCreating(true)
        }}
        css={{ display: 'flex', marginRight: spacing.normal }}
      >
        <div css={{ display: 'flex', alignItems: 'center' }}>
          <div css={{ display: 'flex', paddingRight: spacing.small }}>
            <PlusSvg height={constants.iconSize} />
          </div>
          Add Chart
        </div>
      </Button>

      <Button
        variant={VARIANTS.SECONDARY_SMALL}
        onClick={() => {
          dispatch({ type: ACTIONS.CLEAR })

          setLayouts({ value: {} })

          setIsCreating(true)
        }}
        css={{ marginRight: spacing.normal }}
      >
        Delete All
      </Button>
    </div>
  )
}

DataVisualizationActions.propTypes = {
  dispatch: PropTypes.func.isRequired,
  setIsCreating: PropTypes.func.isRequired,
  setLayouts: PropTypes.func.isRequired,
}

export default DataVisualizationActions
