import useSWR from 'swr'
import PropTypes from 'prop-types'

import { colors, constants, spacing, typography } from '../Styles'

const SIZE = 32

const ModelAssetsContent = ({ projectId, query }) => {
  const { data } = useSWR(
    `/api/v1/projects/${projectId}/searches/query/?query=${query}&from=${0}&size=${SIZE}`,
  )

  const { results = [], count } = data || {}

  return (
    <>
      <h3
        css={{
          color: colors.structure.zinc,
          fontSize: typography.size.regular,
          lineHeight: typography.height.regular,
          fontWeight: typography.weight.regular,
          paddingBottom: spacing.normal,
        }}
      >
        Number of Assets: {count}
      </h3>

      <div
        css={{
          display: 'grid',
          gridTemplateColumns: 'repeat(8, minmax(0, 1fr))',
          gap: 1,
        }}
      >
        {results.map(({ id, name, thumbnailUrl }) => {
          const { pathname: thumbnailSrc } = new URL(thumbnailUrl)

          return (
            <div
              key={id}
              css={{
                position: 'relative',
                border: constants.borders.large.transparent,
                width: '100%',
                height: '100%',
                ':hover': {
                  border: constants.borders.large.white,
                },
              }}
            >
              <div
                css={{
                  width: '100%',
                  height: '100%',
                  minWidth: '100%',
                  paddingBottom: '100%',
                  position: 'relative',
                  display: 'flex',
                  justifyContent: 'center',
                  alignItems: 'center',
                  background: colors.structure.mattGrey,
                }}
              >
                <img
                  css={{
                    position: 'absolute',
                    top: 0,
                    width: '100%',
                    height: '100%',
                    objectFit: 'contain',
                  }}
                  src={thumbnailSrc}
                  alt={name}
                />
              </div>
            </div>
          )
        })}
      </div>
    </>
  )
}

ModelAssetsContent.propTypes = {
  projectId: PropTypes.string.isRequired,
  query: PropTypes.string.isRequired,
}

export default ModelAssetsContent
