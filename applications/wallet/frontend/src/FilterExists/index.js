import PropTypes from 'prop-types'

import { spacing, constants, colors } from '../Styles'

import Accordion, { VARIANTS as ACCORDION_VARIANTS } from '../Accordion'
import Button, { VARIANTS } from '../Button'
import FiltersReset from '../Filters/Reset'

import { dispatch, ACTIONS } from '../Filters/helpers'

const FilterExists = ({
  projectId,
  assetId,
  filters,
  filter,
  filter: { attribute, values: { exists = true } = {} },
  filterIndex,
}) => {
  return (
    <Accordion
      variant={ACCORDION_VARIANTS.PANEL}
      title={attribute}
      isInitiallyOpen
    >
      <div
        css={{
          padding: spacing.normal,
          paddingLeft: spacing.moderate,
          paddingRight: spacing.moderate,
        }}
      >
        <FiltersReset
          projectId={projectId}
          assetId={assetId}
          filters={filters}
          filter={filter}
          filterIndex={filterIndex}
          onReset={() => {}}
        />
        <div css={{ paddingTop: spacing.base, paddingBottom: spacing.base }}>
          <div
            css={{
              display: 'flex',
              border: constants.borders.tableRow,
              borderRadius: constants.borderRadius.small,
            }}
          >
            {['Exists', 'Missing'].map((value) => (
              <Button
                key={value}
                style={{
                  flex: 1,
                  borderRadius: 0,
                  padding: spacing.moderate,
                  backgroundColor:
                    (value === 'Exists' && exists) ||
                    (value === 'Missing' && !exists)
                      ? colors.structure.steel
                      : colors.transparent,
                  color:
                    (value === 'Exists' && exists) ||
                    (value === 'Missing' && !exists)
                      ? colors.structure.white
                      : colors.structure.steel,
                  ':hover': {
                    color: colors.structure.white,
                  },
                }}
                variant={VARIANTS.NEUTRAL}
                onClick={() =>
                  dispatch({
                    action: ACTIONS.UPDATE_FILTER,
                    payload: {
                      projectId,
                      assetId,
                      filters,
                      updatedFilter: {
                        ...filter,
                        values: { exists: value === 'Exists' || false },
                      },
                      filterIndex,
                    },
                  })
                }
              >
                {value}
              </Button>
            ))}
          </div>
          <div
            css={{
              color: colors.structure.steel,
              textAlign: 'center',
              padding: spacing.base,
            }}
          >
            {exists ? (
              <span>Show assets with the field &quot;{attribute}&quot;</span>
            ) : (
              <span>
                Show assets <u>missing</u> the field &quot;{attribute}&quot;
              </span>
            )}
          </div>
        </div>
      </div>
    </Accordion>
  )
}

FilterExists.propTypes = {
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
  filters: PropTypes.arrayOf(
    PropTypes.shape({
      type: PropTypes.oneOf(['search', 'facet', 'range', 'exists']).isRequired,
      attribute: PropTypes.string,
      values: PropTypes.oneOfType([PropTypes.string, PropTypes.object]),
    }).isRequired,
  ).isRequired,
  filter: PropTypes.shape({
    type: PropTypes.oneOf(['exists']).isRequired,
    attribute: PropTypes.string.isRequired,
    values: PropTypes.shape({ exists: PropTypes.bool }),
  }).isRequired,
  filterIndex: PropTypes.number.isRequired,
}

export default FilterExists
