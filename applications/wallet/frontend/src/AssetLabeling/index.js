import { useState } from 'react'
import { useRouter } from 'next/router'

import { colors, spacing, typography } from '../Styles'

import SuspenseBoundary from '../SuspenseBoundary'
import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import BulkAssetLabeling from '../BulkAssetLabeling'
import {
  ACTIONS as FILTER_ACTIONS,
  dispatch as filterDispatch,
  decode,
} from '../Filters/helpers'

import AssetLabelingContent from './Content'

const AssetLabeling = () => {
  const [isBulkLabeling, setIsBulkLabeling] = useState(false)

  const {
    pathname,
    query: { projectId, assetId, query },
  } = useRouter()

  const filters = decode({ query })

  if (assetId) {
    return (
      <SuspenseBoundary>
        <AssetLabelingContent
          projectId={projectId}
          assetId={assetId}
          setIsBulkLabeling={setIsBulkLabeling}
        />
      </SuspenseBoundary>
    )
  }

  if (isBulkLabeling) {
    return (
      <BulkAssetLabeling
        projectId={projectId}
        query={query}
        setIsBulkLabeling={setIsBulkLabeling}
      />
    )
  }

  return (
    <div
      css={{
        padding: spacing.normal,
        color: colors.structure.zinc,
        fontStyle: typography.style.italic,
      }}
    >
      <h5
        css={{
          margin: 0,
          fontSize: typography.size.regular,
          lineHeight: typography.height.regular,
        }}
      >
        Label a Single Asset:
      </h5>
      <p
        css={{
          marginTop: 0,
          fontSize: typography.size.regular,
          lineHeight: typography.height.regular,
        }}
      >
        Select an asset in the asset viewer to the left to label individually.{' '}
      </p>
      <h5
        css={{
          margin: 0,
          fontSize: typography.size.regular,
          lineHeight: typography.height.regular,
        }}
      >
        Label All Assets in Search:
      </h5>
      <p
        css={{
          margin: 0,
          fontSize: typography.size.regular,
          lineHeight: typography.height.regular,
        }}
      >
        Click the button below to label all assets in search. Video assets will
        be excluded from labeling and a maximum of 10,000 assets can be labeled
        at once.
      </p>
      <p
        css={{
          marginTop: 0,
          fontSize: typography.size.regular,
          lineHeight: typography.height.regular,
        }}
      >
        Filters automatically added:
      </p>
      <ul>
        <li>File Type (video deselected)</li>
        <li>Search limit</li>
      </ul>

      <div css={{ height: spacing.normal }} />

      <Button
        variant={BUTTON_VARIANTS.PRIMARY}
        onClick={() => {
          const limitFilter = filters.find(({ type }) => type === 'limit')

          const mediaTypeFilter = filters.find(
            ({ attribute }) => attribute === 'media.type',
          )

          filterDispatch({
            type: FILTER_ACTIONS.ADD_FILTERS,
            payload: {
              pathname,
              projectId,
              assetId,
              filters,
              newFilters: [
                !limitFilter
                  ? {
                      type: 'limit',
                      attribute: 'utility.Search Results Limit',
                      values: { maxAssets: 10_000 },
                    }
                  : false,
                !mediaTypeFilter
                  ? {
                      type: 'facet',
                      attribute: 'media.type',
                      values: { facets: ['image', 'document'] },
                    }
                  : false,
              ].filter(Boolean),
              query,
            },
          })

          setIsBulkLabeling(true)
        }}
      >
        Bulk Label All Images &amp; Documents in Search
      </Button>
    </div>
  )
}

export default AssetLabeling
