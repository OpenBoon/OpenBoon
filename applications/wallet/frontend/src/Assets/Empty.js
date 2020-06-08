import Router from 'next/router'
import PropTypes from 'prop-types'

import { colors, spacing, typography } from '../Styles'

import NoAssetsSvg from '../Icons/noAssets.svg'

import Button, { VARIANTS } from '../Button'

import { ACTIONS, dispatch, decode } from '../Filters/helpers'

const AssetsEmpty = ({ projectId, query, assetId }) => {
  const filters = query ? decode({ query }) : []
  const hasFilters = filters.length > 0

  return (
    <div
      css={{
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'center',
        alignItems: 'center',
        padding: spacing.normal,
        textAlign: 'center',
      }}
    >
      <NoAssetsSvg width={168} color={colors.structure.steel} />
      <h2
        css={{
          paddingTop: spacing.normal,
          fontSize: typography.size.giant,
          lineHeight: typography.height.giant,
        }}
      >
        There are currently no assets to show.
      </h2>
      <h3
        css={{
          fontSize: typography.size.large,
          lineHeight: typography.height.large,
          fontWeight: typography.weight.regular,
          color: colors.structure.zinc,
        }}
      >
        Either all have been filtered out or there aren’t any in the system yet.
      </h3>
      {hasFilters && (
        <>
          <div css={{ height: spacing.comfy }} />
          <Button
            aria-label="Clear All Filters"
            variant={VARIANTS.PRIMARY}
            onClick={() => {
              dispatch({
                action: ACTIONS.CLEAR_FILTERS,
                payload: { projectId, assetId },
              })
            }}
          >
            Clear All Filters
          </Button>
        </>
      )}

      {!hasFilters && (
        <>
          <div css={{ height: spacing.comfy }} />
          <Button
            aria-label="Create a Data Source"
            variant={VARIANTS.PRIMARY}
            onClick={() => {
              Router.push(
                '/[projectId]/data-sources/add',
                `/${projectId}/data-sources/add`,
              )
              return null
            }}
          >
            Create a Data Source
          </Button>
          <div css={{ height: spacing.comfy }} />
          <Button
            aria-label="View Job Queue"
            variant={VARIANTS.PRIMARY}
            onClick={() => {
              Router.push('/[projectId]/jobs', `/${projectId}/jobs`)
              return null
            }}
          >
            View Job Queue
          </Button>
        </>
      )}
    </div>
  )
}

AssetsEmpty.defaultProps = {
  query: '',
  assetId: '',
}

AssetsEmpty.propTypes = {
  projectId: PropTypes.string.isRequired,
  query: PropTypes.string,
  assetId: PropTypes.string,
}

export default AssetsEmpty
