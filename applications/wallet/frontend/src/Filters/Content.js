/* eslint-disable react/no-array-index-key */
import PropTypes from 'prop-types'

import filterShape from '../Filter/shape'

import { spacing, constants } from '../Styles'

import PlusSvg from '../Icons/plus.svg'

import SearchFilter from '../SearchFilter'
import Button, { VARIANTS } from '../Button'
import FilterText from '../FilterText'
import FilterExists from '../FilterExists'
import FilterFacet from '../FilterFacet'
import FilterRange from '../FilterRange'
import FilterLabelConfidence from '../FilterLabelConfidence'
import FilterSimilarity from '../FilterSimilarity'

import { dispatch, ACTIONS } from './helpers'

const BUTTON_SIZE = 190
const ICON_SIZE = 20

const FiltersContent = ({ projectId, assetId, filters, setIsMenuOpen }) => {
  const hasFilters = filters.length > 0

  return (
    <>
      <div
        css={{
          padding: spacing.small,
          borderBottom: constants.borders.divider,
        }}
      >
        <div css={{ display: 'flex' }}>
          <Button
            aria-label="Add Metadata Filters"
            variant={VARIANTS.PRIMARY}
            style={{
              flex: 1,
              minWidth: BUTTON_SIZE,
              maxWidth: !hasFilters ? BUTTON_SIZE : '',
            }}
            onClick={() => setIsMenuOpen((isMenuOpen) => !isMenuOpen)}
          >
            <div css={{ display: 'flex', alignItems: 'center' }}>
              <div css={{ display: 'flex', paddingRight: spacing.small }}>
                <PlusSvg width={ICON_SIZE} />
              </div>
              Add Metadata Filters
            </div>
          </Button>

          <div css={{ width: spacing.base, minWidth: spacing.base }} />

          {hasFilters && (
            <Button
              aria-label="Clear All Filters"
              variant={VARIANTS.SECONDARY}
              style={{ flex: 1, minWidth: BUTTON_SIZE }}
              isDisabled={filters.length === 0}
              onClick={() => {
                dispatch({
                  action: ACTIONS.CLEAR_FILTERS,
                  payload: { projectId, assetId },
                })
              }}
            >
              <div css={{ height: ICON_SIZE }}>Clear All Filters</div>
            </Button>
          )}
        </div>

        <div css={{ height: spacing.small }} />

        <SearchFilter
          projectId={projectId}
          assetId={assetId}
          filters={filters}
        />
      </div>

      <div css={{ overflow: 'auto' }}>
        {filters.map((filter, index) => {
          switch (filter.type) {
            case 'exists':
              return (
                <FilterExists
                  key={`${filter.type}-${index}`}
                  projectId={projectId}
                  assetId={assetId}
                  filters={filters}
                  filter={filter}
                  filterIndex={index}
                />
              )

            case 'facet':
              return (
                <FilterFacet
                  key={`${filter.type}-${index}`}
                  projectId={projectId}
                  assetId={assetId}
                  filters={filters}
                  filter={filter}
                  filterIndex={index}
                />
              )

            case 'range':
              return (
                <FilterRange
                  key={`${filter.type}-${index}`}
                  projectId={projectId}
                  assetId={assetId}
                  filters={filters}
                  filter={filter}
                  filterIndex={index}
                />
              )

            case 'labelConfidence':
              return (
                <FilterLabelConfidence
                  key={`${filter.type}-${index}`}
                  projectId={projectId}
                  assetId={assetId}
                  filters={filters}
                  filter={filter}
                  filterIndex={index}
                />
              )

            case 'textContent':
              return (
                <FilterText
                  key={`${filter.type}-${index}`}
                  projectId={projectId}
                  assetId={assetId}
                  filters={filters}
                  filter={filter}
                  filterIndex={index}
                />
              )

            case 'similarity':
              return (
                <FilterSimilarity
                  key={`${filter.type}-${index}`}
                  projectId={projectId}
                  assetId={assetId}
                  filters={filters}
                  filter={filter}
                  filterIndex={index}
                />
              )

            default:
              return null
          }
        })}
      </div>
    </>
  )
}

FiltersContent.propTypes = {
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
  filters: PropTypes.arrayOf(PropTypes.shape(filterShape)).isRequired,
  setIsMenuOpen: PropTypes.func.isRequired,
}

export default FiltersContent
