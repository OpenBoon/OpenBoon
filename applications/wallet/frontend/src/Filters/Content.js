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
import FilterDateRange from '../FilterDateRange'

import { dispatch, ACTIONS } from './helpers'

const BUTTON_SIZE = 190
const FiltersContent = ({
  pathname,
  projectId,
  assetId,
  filters,
  setIsMenuOpen,
}) => {
  const hasFilters = filters.length > 0

  return (
    <>
      <div
        css={{
          padding: spacing.small,
          borderBottom: constants.borders.regular.smoke,
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
                <PlusSvg height={constants.icons.regular} />
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
                  type: ACTIONS.CLEAR_FILTERS,
                  payload: { pathname, projectId, assetId },
                })
              }}
            >
              <div css={{ height: constants.icons.regular }}>
                Clear All Filters
              </div>
            </Button>
          )}
        </div>

        <div css={{ height: spacing.small }} />

        <SearchFilter
          pathname={pathname}
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
                  pathname={pathname}
                  projectId={projectId}
                  assetId={assetId}
                  filters={filters}
                  filter={filter}
                  filterIndex={index}
                />
              )

            case 'facet':
            case 'label':
              return (
                <FilterFacet
                  key={`${filter.type}-${index}`}
                  pathname={pathname}
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
                  pathname={pathname}
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
                  pathname={pathname}
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
                  pathname={pathname}
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
                  pathname={pathname}
                  projectId={projectId}
                  assetId={assetId}
                  filters={filters}
                  filter={filter}
                  filterIndex={index}
                />
              )

            case 'date':
              return (
                <FilterDateRange
                  key={`${filter.type}-${index}`}
                  pathname={pathname}
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
  pathname: PropTypes.string.isRequired,
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
  filters: PropTypes.arrayOf(PropTypes.shape(filterShape)).isRequired,
  setIsMenuOpen: PropTypes.func.isRequired,
}

export default FiltersContent
