import PropTypes from 'prop-types'
import { useRouter } from 'next/router'
import useSWR from 'swr'

import { colors, constants, spacing, typography } from '../Styles'

import { encode } from '../Filters/helpers'

import ButtonCopy, { COPY_SIZE } from '../Button/Copy'
import AssetsThumbnail from '../Assets/Thumbnail'

const THUMNAIL_SIZE = 80

const MetadataPrettySimilarity = ({ name, value: { simhash } }) => {
  const {
    query: { projectId, id: assetId },
  } = useRouter()

  const query = encode({
    filters: [
      {
        type: 'similarity',
        attribute: 'analysis.zvi-image-similarity',
        // TODO: replace `hashes` with `ids` after backend update
        values: { hashes: [simhash] },
      },
    ],
  })

  const {
    data: { results },
  } = useSWR(
    `/api/v1/projects/${projectId}/searches/query/?query=${query}&from=0&size=10`,
  )

  return (
    <>
      <div
        css={{
          '&:not(:first-of-type)': {
            borderTop: constants.borders.largeDivider,
          },
          padding: spacing.normal,
          paddingBottom: spacing.base,
          fontFamily: 'Roboto Mono',
          fontSize: typography.size.small,
          lineHeight: typography.height.small,
          color: colors.structure.white,
        }}
      >
        {name}
      </div>
      <div
        css={{
          padding: `${spacing.base}px ${spacing.normal}px`,
          paddingBottom: 0,
          minHeight: COPY_SIZE,
          width: '100%',
          fontFamily: 'Roboto Condensed',
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
            ':hover': {
              backgroundColor: colors.signal.electricBlue.background,
              div: {
                svg: {
                  display: 'inline-block',
                },
              },
            },
          }}
        >
          <div
            css={{
              padding: `${spacing.moderate}px ${spacing.normal}px`,
              fontFamily: 'Roboto Mono',
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
          <div
            css={{
              minWidth: COPY_SIZE + spacing.normal,
              paddingTop: spacing.moderate,
              paddingRight: spacing.normal,
            }}
          >
            <ButtonCopy value={simhash} />
          </div>
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
                borderTop: constants.borders.divider,
                paddingTop: spacing.normal,
                paddingBottom: spacing.moderate,
                minHeight: COPY_SIZE,
                width: '100%',
                fontFamily: 'Roboto Condensed',
                textTransform: 'uppercase',
                color: colors.structure.steel,
              }}
            >
              Similar Images
            </div>
            <div
              css={{ display: 'flex', flexWrap: 'nowrap', overflow: 'hidden' }}
            >
              {results
                .filter(({ id }) => id !== assetId)
                .map((asset) => (
                  <div
                    key={asset.id}
                    css={{
                      width: THUMNAIL_SIZE,
                      minWidth: THUMNAIL_SIZE,
                      height: THUMNAIL_SIZE,
                    }}
                  >
                    <AssetsThumbnail asset={asset} />
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
}

export default MetadataPrettySimilarity
