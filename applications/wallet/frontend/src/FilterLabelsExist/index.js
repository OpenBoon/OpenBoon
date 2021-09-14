import PropTypes from 'prop-types'

import filterShape from '../Filter/shape'

import { spacing, constants, colors } from '../Styles'

import ExistsSvg from '../Icons/exists.svg'
import MissingSvg from '../Icons/missing.svg'

import { dispatch, ACTIONS } from '../Filters/helpers'

import Accordion, { VARIANTS as ACCORDION_VARIANTS } from '../Accordion'
import Button, { VARIANTS } from '../Button'
import FilterReset from '../Filter/Reset'
import FilterIcon from '../Filter/Icon'
import FilterActions from '../Filter/Actions'

export const noop = () => {}

const FilterLabelsExist = ({
  pathname,
  projectId,
  assetId,
  filters,
  filter,
  filter: { attribute, values: { exists = true } = {} },
  filterIndex,
}) => {
  const datasetName = filter.attribute.split('.')[1]

  return (
    <Accordion
      variant={ACCORDION_VARIANTS.FILTER}
      icon={<FilterIcon filter={filter} />}
      title={filter.attribute}
      actions={
        <FilterActions
          pathname={pathname}
          projectId={projectId}
          assetId={assetId}
          filters={filters}
          filter={filter}
          filterIndex={filterIndex}
        />
      }
      cacheKey={`FilterLabelsExist.${attribute}`}
      isInitiallyOpen
      isResizeable
    >
      <div css={{ padding: spacing.normal }}>
        <FilterReset
          pathname={pathname}
          projectId={projectId}
          assetId={assetId}
          filters={filters}
          filter={filter}
          filterIndex={filterIndex}
          onReset={noop}
        />
        <div
          css={{
            paddingTop: spacing.base,
            paddingLeft: spacing.normal,
            paddingRight: spacing.normal,
          }}
        >
          <div
            css={{
              display: 'flex',
              border: constants.borders.regular.steel,
              borderRadius: constants.borderRadius.small,
            }}
          >
            {['Exists', 'Missing'].map((value) => (
              <Button
                variant={VARIANTS.ICON}
                aria-label={value}
                key={value}
                style={{
                  flex: 1,
                  borderRadius: 0,
                  padding: spacing.moderate,
                  backgroundColor:
                    (value === 'Exists' && exists) ||
                    (value === 'Missing' && !exists)
                      ? colors.structure.steel
                      : colors.structure.transparent,
                  color:
                    (value === 'Exists' && exists) ||
                    (value === 'Missing' && !exists)
                      ? colors.structure.white
                      : colors.structure.steel,
                }}
                onClick={() =>
                  dispatch({
                    type: ACTIONS.UPDATE_FILTER,
                    payload: {
                      pathname,
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
                <div css={{ display: 'flex', alignItems: 'center' }}>
                  {value === 'Exists' ? (
                    <ExistsSvg height={constants.icons.regular} />
                  ) : (
                    <MissingSvg height={constants.icons.regular} />
                  )}
                  <div css={{ paddingLeft: spacing.small }}>{value}</div>
                </div>
              </Button>
            ))}
          </div>
          <div
            css={{
              color: colors.structure.steel,
              textAlign: 'center',
              paddingTop: spacing.base,
            }}
          >
            {exists ? (
              <span>Show assets with a &quot;{datasetName}&quot; label</span>
            ) : (
              <span>
                Show assets <u>missing</u> a &quot;{datasetName}&quot; label
              </span>
            )}
          </div>
        </div>
      </div>
    </Accordion>
  )
}

FilterLabelsExist.propTypes = {
  pathname: PropTypes.string.isRequired,
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
  filters: PropTypes.arrayOf(PropTypes.shape(filterShape)).isRequired,
  filter: PropTypes.shape(filterShape).isRequired,
  filterIndex: PropTypes.number.isRequired,
}

export default FilterLabelsExist
