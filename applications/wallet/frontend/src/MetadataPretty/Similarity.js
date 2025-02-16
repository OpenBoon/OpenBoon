import PropTypes from 'prop-types'
import { useRouter } from 'next/router'
import useSWR from 'swr'

import { colors, constants, spacing, typography } from '../Styles'

import {
  encode,
  ACTIONS as FILTER_ACTIONS,
  dispatch as filterDispatch,
} from '../Filters/helpers'

import ButtonCopy from '../Button/Copy'
import AssetsThumbnail from '../Assets/Thumbnail'
import Button, { VARIANTS } from '../Button'

const THUMBNAIL_SIZE = 100

const MetadataPrettySimilarity = ({ name, value: { simhash }, path }) => {
  const {
    query: { projectId, assetId, query: q },
  } = useRouter()

  const query = encode({
    filters: [
      {
        type: 'similarity',
        attribute: `${path}.${name}`,
        values: { ids: [assetId] },
      },
    ],
  })

  const { data } = useSWR(
    `/api/v1/projects/${projectId}/searches/query/?query=${query}&from=0&size=10`,
  )

  const { results = [] } = data || {}

  return (
    <>
      <div
        css={{
          padding: spacing.normal,
          paddingBottom: spacing.base,
          fontWeight: typography.weight.bold,
          fontSize: typography.size.regular,
          lineHeight: typography.height.regular,
          color: colors.key.two,
        }}
      >
        <Button
          aria-label="Add Filter"
          variant={VARIANTS.NEUTRAL}
          style={{
            fontWeight: 'inherit',
            fontSize: 'inherit',
            lineHeight: 'inherit',
            color: 'inherit',
          }}
          onClick={() => {
            filterDispatch({
              type: FILTER_ACTIONS.APPLY_SIMILARITY,
              payload: {
                projectId,
                thumbnailId: assetId,
                assetId,
                query: q,
                attribute: `${path}.${name}`,
              },
            })
          }}
        >
          {name}
        </Button>
      </div>

      <div
        css={{
          padding: `${spacing.base}px ${spacing.normal}px`,
          paddingBottom: 0,
          width: '100%',
          fontFamily: typography.family.condensed,
          textTransform: 'uppercase',
          color: colors.structure.steel,
        }}
      >
        simhash
      </div>

      <div css={{ paddingBottom: spacing.base }}>
        <div
          css={{
            display: 'flex',
            alignItems: 'center',
            padding: spacing.small,
            paddingLeft: spacing.normal,
            paddingRight: spacing.moderate,
            svg: { opacity: 0 },
            ':hover': {
              backgroundColor: `${colors.signal.sky.base}${constants.opacity.hex22Pct}`,
              svg: { opacity: 1 },
            },
          }}
        >
          <div
            css={{
              fontFamily: typography.family.mono,
              fontSize: typography.size.small,
              lineHeight: typography.height.small,
              color: colors.structure.white,
              overflow: 'hidden',
              textOverflow: 'ellipsis',
            }}
            title={simhash}
          >
            {simhash}
          </div>

          <ButtonCopy title="Simhash" value={simhash} offset={100} />
        </div>

        {results.length > 1 && (
          <div
            css={{
              padding: spacing.normal,
              paddingTop: spacing.small,
              paddingBottom: spacing.moderate,
            }}
          >
            <div
              css={{
                borderTop: constants.borders.regular.smoke,
                paddingTop: spacing.normal,
                paddingBottom: spacing.moderate,
                fontFamily: typography.family.condensed,
                textTransform: 'uppercase',
                color: colors.structure.steel,
              }}
            >
              Similar Images
            </div>

            <div
              css={{
                display: 'flex',
                flexWrap: 'nowrap',
                overflowX: 'scroll',
                '::-webkit-scrollbar': {
                  display: 'none',
                },
              }}
            >
              {results
                .filter(({ id }) => id !== assetId)
                .map((asset) => (
                  <div
                    key={asset.id}
                    css={{
                      width: THUMBNAIL_SIZE,
                      minWidth: THUMBNAIL_SIZE,
                      height: THUMBNAIL_SIZE,
                    }}
                  >
                    <AssetsThumbnail
                      asset={asset}
                      isActive
                      attribute={`${path}.${name}`}
                    />
                  </div>
                ))}
            </div>
          </div>
        )}
      </div>
    </>
  )
}

MetadataPrettySimilarity.propTypes = {
  name: PropTypes.string.isRequired,
  value: PropTypes.shape({
    simhash: PropTypes.string.isRequired,
  }).isRequired,
  path: PropTypes.string.isRequired,
}

export default MetadataPrettySimilarity
