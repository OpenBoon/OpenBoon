import { useRouter } from 'next/router'
import useSWR from 'swr'

import { spacing, typography } from '../Styles'

const LINE_HEIGHT = '23px'

const OrganizationUserProjectsDetails = () => {
  const {
    query: { organizationId, userId },
  } = useRouter()

  const { data: user } = useSWR(
    `/api/v1/organizations/${organizationId}/users/${userId}/`,
  )

  return (
    <div css={{ display: 'flex', padding: spacing.comfy, paddingLeft: 0 }}>
      <ul
        css={{
          margin: 0,
          padding: 0,
          listStyle: 'none',
          fontSize: typography.size.medium,
          lineHeight: LINE_HEIGHT,
        }}
      >
        <li>
          <strong>User:</strong> {user.firstName} {user.lastName}
        </li>

        <li>
          <strong>Email:</strong> {user.email}
        </li>
      </ul>
    </div>
  )
}

export default OrganizationUserProjectsDetails
