def qsql(email_id, video_id, base_url, frame_count, current_row):
    sql_statements = []
    for frame_index in range(1, frame_count + 1):
        frame_url = f"{base_url}v1e{frame_index}.jpg"
        sql_statement = f"INSERT INTO enhanced_frame (FRAME_ID, EMAIL_ID, VIDEO_ID, ENHANCED_FRAMES_URL) " \
                        f"VALUES ('{current_row + frame_index}', '{email_id}', '{video_id}', '{frame_url}');"
        sql_statements.append(sql_statement)
    return sql_statements

def captured_frame(email_id, video_id, base_url, frame_count, current_row):
    sql_statements = []
    for frame_index in range(1, frame_count + 1):
        frame_url = f"{base_url}v1c{frame_index}.jpg"
        sql_statement = f"INSERT INTO captured_frame (FRAME_ID, EMAIL_ID, VIDEO_ID, CAPTURED_FRAMES_URL) " \
                        f"VALUES ('{current_row + frame_index}', '{email_id}', '{video_id}', '{frame_url}');"
        sql_statements.append(sql_statement)
    return sql_statements

def record_video(email_id, video_id, base_url, enha_url, frame_count, current_row):
    sql_statements = []
    for frame_index in range(1, frame_count + 1):
        frame_url1 = f"{enha_url}v1e{frame_index}.jpg"
        frame_url2 = f"{base_url}v1c{frame_index}.jpg"
        sql_statement = f"INSERT INTO record_video (FRAME_ID, EMAIL, VIDEO_ID, CAPTURED_FRAME, ENHANCED_FRAME) " \
                        f"VALUES ('{current_row + frame_index}', '{email_id}', '{video_id}', '{frame_url2}', '{frame_url1}');"
        sql_statements.append(sql_statement)
    return sql_statements