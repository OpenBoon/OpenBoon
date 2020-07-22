import { useState } from 'react'
import PropTypes from 'prop-types'
import Link from 'next/link'

import { fetcher } from '../Fetch/helpers'

import Menu from '../Menu'
import Button, { VARIANTS } from '../Button'
import ButtonActions from '../Button/Actions'
import Modal from '../Modal'

const ProjectUsersMenu = ({ projectId, userId, revalidate }) => {
  const [isRemoveModalOpen, setRemoveModalOpen] = useState(false)

  return (
    <Menu open="left" button={ButtonActions}>
      {({ onClick }) => (
        <div>
          <ul>
            <li>
              <Link
                href="/[projectId]/users/[userId]/edit"
                as={`/${projectId}/users/${userId}/edit`}
                passHref
              >
                <Button
                  variant={VARIANTS.MENU_ITEM}
                  onClick={onClick}
                  isDisabled={false}
                >
                  Edit
                </Button>
              </Link>
            </li>
            <li>
              <>
                <Button
                  variant={VARIANTS.MENU_ITEM}
                  onClick={() => {
                    setRemoveModalOpen(true)
                  }}
                  isDisabled={false}
                >
                  Remove
                </Button>
                {isRemoveModalOpen && (
                  <Modal
                    title="Remove User from Project"
                    message="Are your sure you want to remove this user?"
                    action="Remove User"
                    onCancel={() => {
                      setRemoveModalOpen(false)
                      onClick()
                    }}
                    onConfirm={async () => {
                      setRemoveModalOpen(false)
                      onClick()

                      await fetcher(
                        `/api/v1/projects/${projectId}/users/${userId}/`,
                        { method: 'DELETE' },
                      )

                      revalidate()
                    }}
                  />
                )}
              </>
            </li>
          </ul>
        </div>
      )}
    </Menu>
  )
}

ProjectUsersMenu.propTypes = {
  projectId: PropTypes.string.isRequired,
  userId: PropTypes.number.isRequired,
  revalidate: PropTypes.func.isRequired,
}

export default ProjectUsersMenu
